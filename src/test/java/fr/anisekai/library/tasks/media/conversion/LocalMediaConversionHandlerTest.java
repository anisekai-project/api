package fr.anisekai.library.tasks.media.conversion;

import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.domain.keys.TorrentKey;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.wireless.tasks.conversion.MediaConversionInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalMediaConversionHandlerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesSourceUnderImports() throws Exception {

        Path imports = Files.createDirectory(this.temporaryDirectory.resolve("imports"));
        Path source = Files.writeString(imports.resolve("episode.mkv"), "episode");
        Library library = mock(Library.class);
        StorageResolver resolver = mock(StorageResolver.class);
        when(library.getResolver(Library.IMPORTS)).thenReturn(resolver);
        when(resolver.directory()).thenReturn(imports);

        LocalMediaConversionHandler handler = new LocalMediaConversionHandler(
                library,
                mock(EpisodeService.class),
                mock(TorrentFileService.class)
        );

        assertEquals(source.toRealPath(), handler.fetchEpisode(episodeReference(UUID.randomUUID())));
    }

    @Test
    void resolvesSourceUnderDownloads() throws Exception {

        Path downloads = Files.createDirectory(this.temporaryDirectory.resolve("downloads"));
        Path source = Files.writeString(downloads.resolve("episode.mkv"), "episode");
        UUID torrentId = UUID.randomUUID();

        Torrent torrent = new Torrent();
        torrent.setId(torrentId);

        TorrentFile torrentFile = new TorrentFile();
        torrentFile.setTorrent(torrent);
        torrentFile.setIndex(3);
        torrentFile.setName("episode.mkv");

        Library library = mock(Library.class);
        StorageResolver resolver = mock(StorageResolver.class);
        when(library.getResolver(Library.DOWNLOADS)).thenReturn(resolver);
        when(resolver.directory()).thenReturn(downloads);
        when(library.findDownload(torrentFile)).thenReturn(Optional.of(source));

        TorrentFileService torrentFiles = mock(TorrentFileService.class);
        when(torrentFiles.requireById(new TorrentKey(torrentId, 3))).thenReturn(torrentFile);

        LocalMediaConversionHandler handler = new LocalMediaConversionHandler(
                library,
                mock(EpisodeService.class),
                torrentFiles
        );

        MediaConversionInput.Episode reference = new MediaConversionInput.Episode(
                UUID.randomUUID(),
                new MediaConversionInput.Source(MediaConversionInput.Store.DOWNLOADS, torrentId + "/3"),
                "0".repeat(64)
        );

        assertEquals(source.toRealPath(), handler.fetchEpisode(reference));
    }

    @Test
    void publishesAndCleansAttemptOwnedIsolation() throws Exception {

        UUID episodeId = UUID.randomUUID();
        Episode episode = new Episode();
        episode.setId(episodeId);
        Library library = mock(Library.class);
        EpisodeService episodeService = mock(EpisodeService.class);
        IsolationSession isolation = mock(IsolationSession.class);
        Path temporaryOutput = this.temporaryDirectory.resolve("isolation/tmp/output.mkv");
        Path publishedOutput = this.temporaryDirectory.resolve("isolation/episodes/episode.mkv");
        Files.createDirectories(temporaryOutput.getParent());
        Files.createDirectories(publishedOutput.getParent());

        when(episodeService.requireById(episodeId)).thenReturn(episode);
        when(library.createIsolation(org.mockito.ArgumentMatchers.<Set<AccessScope>>any())).thenReturn(isolation);
        when(isolation.requestTemporaryFile("mkv")).thenReturn(temporaryOutput);
        when(isolation.resolve(any(AccessScope.class))).thenReturn(publishedOutput);

        LocalMediaConversionHandler handler = new LocalMediaConversionHandler(
                library,
                episodeService,
                mock(TorrentFileService.class)
        );
        MediaConversionInput.Episode reference = episodeReference(episodeId);

        assertEquals(temporaryOutput, handler.getStoragePath(reference, this.temporaryDirectory.resolve("source.mkv")));
        Files.writeString(temporaryOutput, "converted");
        handler.pushEpisode(reference, temporaryOutput);
        handler.cleanupEpisode(reference, null, temporaryOutput);

        assertEquals("converted", Files.readString(publishedOutput));
        verify(isolation).commit();
        verify(isolation).close();
    }

    private static MediaConversionInput.Episode episodeReference(UUID episodeId) {

        return new MediaConversionInput.Episode(
                episodeId,
                new MediaConversionInput.Source(MediaConversionInput.Store.IMPORTS, "episode.mkv"),
                "0".repeat(64)
        );
    }

}
