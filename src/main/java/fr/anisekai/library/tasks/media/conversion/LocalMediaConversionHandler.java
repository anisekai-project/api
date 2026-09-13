package fr.anisekai.library.tasks.media.conversion;

import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.wireless.tasks.conversion.MediaConversionHandler;
import fr.anisekai.wireless.tasks.conversion.MediaConversionInput;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class LocalMediaConversionHandler extends MediaConversionHandler {

    private final Library        library;
    private final EpisodeService episodeService;

    private IsolationSession isolation;
    private AccessScope      episodeScope;

    public LocalMediaConversionHandler(Library library, EpisodeService episodeService) {

        this.library        = library;
        this.episodeService = episodeService;
    }

    @Override
    public Path fetchEpisode(MediaConversionInput.Episode episode) throws IOException {

        Path imports = this.library.getResolver(Library.IMPORTS).directory().toRealPath();
        Path candidate = imports.resolve(episode.sourceReference()).normalize();

        if (!candidate.startsWith(imports)) {
            throw new IllegalArgumentException("Conversion source escapes Library.IMPORTS");
        }

        Path source = candidate.toRealPath();
        if (!source.startsWith(imports) || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Conversion source must be a regular file under Library.IMPORTS");
        }
        return source;
    }

    @Override
    public Path getStoragePath(MediaConversionInput.Episode episode, Path source) {

        if (this.isolation != null) {
            throw new IllegalStateException("Media conversion handler instances cannot be reused");
        }

        Episode entity = this.episodeService.requireById(episode.id());
        this.episodeScope = new AccessScope(Library.EPISODES, entity.getScopedName());
        this.isolation = this.library.createIsolation(Set.of(this.episodeScope));
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
        this.isolation = null;
        this.episodeScope = null;

        if (attempt != null) attempt.close();
    }

}
