package fr.anisekai.web.api;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.library.Library;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.domain.keys.TorrentKey;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.server.services.TorrentService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.web.WebFile;
import fr.anisekai.web.exceptions.WebException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LibraryDownloadTest {

    private static Library library(Path root) {

        ApplicationConfiguration config = new ApplicationConfiguration();
        config.getLibrary().setPath(root.toString());
        return new Library(config);
    }

    private static LibraryController controller(Library library, TorrentService torrents, TorrentFileService files) {

        return new LibraryController(
                library,
                new WebFile(library),
                mock(AnimeService.class),
                mock(EpisodeService.class),
                mock(TrackService.class),
                torrents,
                files
        );
    }

    private static LibraryController controller(Library library) {

        return controller(library, mock(TorrentService.class), mock(TorrentFileService.class));
    }

    private static Torrent torrent(UUID id, double progress) {

        Torrent torrent = new Torrent();
        torrent.setId(id);
        torrent.setName("torrent");
        torrent.setHash("0".repeat(40));
        torrent.setLink("magnet:?xt=urn:btih:" + "0".repeat(40));
        torrent.setDownloadDirectory("downloads");
        torrent.setProgress(progress);
        return torrent;
    }

    private static TorrentFile torrentFile(Torrent torrent, int index, String name, boolean removed) {

        TorrentFile file = new TorrentFile();
        file.setTorrent(torrent);
        file.setIndex(index);
        file.setName(name);
        file.setRemoved(removed);
        return file;
    }

    private static byte[] bodyOf(ResponseEntity<InputStreamResource> response) throws Exception {

        assertEquals(HttpStatus.OK, response.getStatusCode());
        try (var in = response.getBody().getInputStream()) {
            return in.readAllBytes();
        }
    }

    @Test
    void servesImportFile(@TempDir Path root) throws Exception {

        Library library = library(root);
        Path imports = library.getResolver(Library.IMPORTS).directory();
        byte[] payload = "import-bytes".getBytes(StandardCharsets.UTF_8);
        Files.write(imports.resolve("episode.mkv"), payload);

        ResponseEntity<InputStreamResource> response = controller(library).getImportFile("episode.mkv");

        assertArrayEquals(payload, bodyOf(response));
        library.close();
    }

    @Test
    void servesImportFileFromDirectory(@TempDir Path root) throws Exception {

        Library library = library(root);
        Path imports = library.getResolver(Library.IMPORTS).directory();
        Files.createDirectories(imports.resolve("batch"));
        byte[] payload = "import-bytes".getBytes(StandardCharsets.UTF_8);
        Files.write(imports.resolve("batch/episode.mkv"), payload);

        ResponseEntity<InputStreamResource> response = controller(library).getImportFile("batch", "episode.mkv");

        assertArrayEquals(payload, bodyOf(response));
        library.close();
    }

    @Test
    void missingImportFileIsNotFound(@TempDir Path root) {

        Library library = library(root);

        ResponseEntity<InputStreamResource> response = controller(library).getImportFile("missing.mkv");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void traversalImportNameIsRejected(@TempDir Path root) {

        Library library = library(root);

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library).getImportFile("../episode.mkv")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
    }

    @Test
    void blankImportNameIsRejected(@TempDir Path root) {

        Library library = library(root);

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library).getImportFile("  ")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
    }

    @Test
    void servesFinishedDownload(@TempDir Path root) throws Exception {

        Library library = library(root);
        Path downloads = library.getResolver(Library.DOWNLOADS).directory();
        byte[] payload = "download-bytes".getBytes(StandardCharsets.UTF_8);
        Files.write(downloads.resolve("episode.mkv"), payload);

        UUID torrentId = UUID.randomUUID();
        Torrent torrent = torrent(torrentId, 1.0);
        TorrentFile file = torrentFile(torrent, 0, "episode.mkv", false);

        TorrentService torrents = mock(TorrentService.class);
        TorrentFileService files = mock(TorrentFileService.class);
        when(torrents.requireById(torrentId)).thenReturn(torrent);
        when(files.requireById(new TorrentKey(torrentId, 0))).thenReturn(file);

        ResponseEntity<InputStreamResource> response = controller(library, torrents, files)
                .getDownloadItem(torrentId, 0);

        assertArrayEquals(payload, bodyOf(response));
        library.close();
    }

    @Test
    void incompleteDownloadIsUnprocessable(@TempDir Path root) {

        Library library = library(root);
        UUID torrentId = UUID.randomUUID();
        Torrent torrent = torrent(torrentId, 0.5);
        TorrentFile file = torrentFile(torrent, 0, "episode.mkv", false);

        TorrentService torrents = mock(TorrentService.class);
        TorrentFileService files = mock(TorrentFileService.class);
        when(torrents.requireById(torrentId)).thenReturn(torrent);
        when(files.requireById(new TorrentKey(torrentId, 0))).thenReturn(file);

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library, torrents, files).getDownloadItem(torrentId, 0)
        );
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.status);
    }

    @Test
    void removedDownloadIsNotFound(@TempDir Path root) {

        Library library = library(root);
        UUID torrentId = UUID.randomUUID();
        Torrent torrent = torrent(torrentId, 1.0);
        TorrentFile file = torrentFile(torrent, 0, "episode.mkv", true);

        TorrentService torrents = mock(TorrentService.class);
        TorrentFileService files = mock(TorrentFileService.class);
        when(torrents.requireById(torrentId)).thenReturn(torrent);
        when(files.requireById(new TorrentKey(torrentId, 0))).thenReturn(file);

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library, torrents, files).getDownloadItem(torrentId, 0)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.status);
    }

    @Test
    void missingDownloadedContentIsNotFound(@TempDir Path root) {

        Library library = library(root);
        UUID torrentId = UUID.randomUUID();
        Torrent torrent = torrent(torrentId, 1.0);
        TorrentFile file = torrentFile(torrent, 0, "absent.mkv", false);

        TorrentService torrents = mock(TorrentService.class);
        TorrentFileService files = mock(TorrentFileService.class);
        when(torrents.requireById(torrentId)).thenReturn(torrent);
        when(files.requireById(new TorrentKey(torrentId, 0))).thenReturn(file);

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library, torrents, files).getDownloadItem(torrentId, 0)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.status);
    }
}
