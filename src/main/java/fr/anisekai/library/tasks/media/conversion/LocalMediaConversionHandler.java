package fr.anisekai.library.tasks.media.conversion;

import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.domain.keys.TorrentKey;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.wireless.tasks.conversion.MediaConversionHandler;
import fr.anisekai.wireless.tasks.conversion.MediaConversionInput;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

public class LocalMediaConversionHandler extends MediaConversionHandler {

    private final Library            library;
    private final EpisodeService     episodeService;
    private final TorrentFileService torrentFileService;

    private IsolationSession isolation;
    private AccessScope      episodeScope;

    public LocalMediaConversionHandler(Library library, EpisodeService episodeService, TorrentFileService torrentFileService) {

        this.library            = library;
        this.episodeService     = episodeService;
        this.torrentFileService = torrentFileService;
    }

    @Override
    public Path fetchEpisode(MediaConversionInput.Episode episode) throws IOException {

        return switch (episode.source().store()) {
            case IMPORTS -> this.resolveImports(episode.source().reference());
            case DOWNLOADS -> this.resolveDownload(episode.source().reference());
        };
    }

    private Path resolveImports(String reference) throws IOException {

        Path imports   = this.library.getResolver(Library.IMPORTS).directory().toRealPath();
        Path candidate = imports.resolve(reference).normalize();

        if (!candidate.startsWith(imports)) {
            throw new IllegalArgumentException("Conversion source escapes Library.IMPORTS");
        }

        Path source = candidate.toRealPath();
        if (!source.startsWith(imports) || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Conversion source must be a regular file under Library.IMPORTS");
        }
        return source;
    }

    private Path resolveDownload(String reference) throws IOException {

        String[] segments = reference.split("/", -1);
        if (segments.length != 2) {
            throw new IllegalArgumentException("Download reference must be '{torrentId}/{fileIndex}'");
        }

        UUID torrentId;
        int  index;
        try {
            torrentId = UUID.fromString(segments[0]);
            index     = Integer.parseInt(segments[1]);
            if (index < 0) throw new IllegalArgumentException("Download file index must be positive");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Download reference must be '{torrentId}/{fileIndex}'", e);
        }

        TorrentFile torrentFile = this.torrentFileService.requireById(new TorrentKey(torrentId, index));
        Path downloads = this.library.getResolver(Library.DOWNLOADS).directory().toRealPath();
        Path source = this.library.findDownload(torrentFile)
                                  .map(candidate -> candidate.toAbsolutePath().normalize())
                                  .filter(candidate -> candidate.startsWith(downloads))
                                  .filter(Files::isRegularFile)
                                  .orElseThrow(() -> new IllegalArgumentException(
                                          "Conversion source must be a regular file under Library.DOWNLOADS"));

        return source.toRealPath();
    }

    @Override
    public Path getStoragePath(MediaConversionInput.Episode episode, Path source) {

        if (this.isolation != null) {
            throw new IllegalStateException("Media conversion handler instances cannot be reused");
        }

        Episode entity = this.episodeService.requireById(episode.id());
        this.episodeScope = new AccessScope(Library.EPISODES, entity.getScopedName());
        this.isolation    = this.library.createIsolation(Set.of(this.episodeScope));
        return this.isolation.requestTemporaryFile("mkv");
    }

    @Override
    public void pushEpisode(MediaConversionInput.Episode episode, Path path) {

        if (this.isolation == null || this.episodeScope == null) {
            throw new IllegalStateException("Media conversion isolation is unavailable");
        }

        try {
            Files.copy(path, this.isolation.resolve(this.episodeScope));
            this.isolation.commit();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to publish converted episode", e);
        }
    }

    @Override
    public void cleanupEpisode(MediaConversionInput.Episode episode, @Nullable Path source, @Nullable Path destination) {

        IsolationSession attempt = this.isolation;
        this.isolation    = null;
        this.episodeScope = null;

        if (attempt != null) attempt.close();
    }

}
