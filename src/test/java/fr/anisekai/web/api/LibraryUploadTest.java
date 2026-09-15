package fr.anisekai.web.api;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.Episode;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LibraryUploadTest {

    private static Library library(Path root) {

        ApplicationConfiguration config = new ApplicationConfiguration();
        config.getLibrary().setPath(root.toString());
        return new Library(config);
    }

    private static Episode episode(UUID id) {

        Episode episode = new Episode();
        episode.setId(id);
        return episode;
    }

    private static LibraryController controller(Library library, EpisodeService episodes) {

        return new LibraryController(
                library,
                mock(WebFile.class),
                mock(AnimeService.class),
                episodes,
                mock(TrackService.class),
                mock(TorrentService.class),
                mock(TorrentFileService.class)
        );
    }

    @Test
    void stagesBodyIntoIsolationWithoutCommitting(@TempDir Path root) throws Exception {

        Library        library   = library(root);
        UUID           episodeId = UUID.randomUUID();
        EpisodeService episodes  = mock(EpisodeService.class);
        when(episodes.requireById(episodeId)).thenReturn(episode(episodeId));

        AccessScope      scope     = new AccessScope(Library.EPISODES, episodeId.toString());
        IsolationSession isolation = library.createIsolation(Set.of(scope));
        byte[]           payload   = "fake-mkv-bytes".getBytes(StandardCharsets.UTF_8);

        ResponseEntity<?> response = controller(library, episodes)
                .stageEpisode(episodeId, isolation, new ByteArrayInputStream(payload));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(payload, Files.readAllBytes(isolation.resolve(scope)));
        assertFalse(Files.exists(library.resolve(scope)));

        isolation.close();
        library.close();
    }

    @Test
    void rejectsMissingIsolation(@TempDir Path root) {

        Library        library   = library(root);
        UUID           episodeId = UUID.randomUUID();
        EpisodeService episodes  = mock(EpisodeService.class);
        when(episodes.requireById(episodeId)).thenReturn(episode(episodeId));

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library, episodes).stageEpisode(
                        episodeId,
                        null,
                        new ByteArrayInputStream(new byte[]{1})
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
    }

    @Test
    void rejectsScopeMismatch(@TempDir Path root) {

        Library        library   = library(root);
        UUID           grantedId = UUID.randomUUID();
        UUID           otherId   = UUID.randomUUID();
        EpisodeService episodes  = mock(EpisodeService.class);
        when(episodes.requireById(otherId)).thenReturn(episode(otherId));

        IsolationSession isolation = library.createIsolation(
                Set.of(new AccessScope(Library.EPISODES, grantedId.toString()))
        );

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library, episodes).stageEpisode(
                        otherId,
                        isolation,
                        new ByteArrayInputStream(new byte[]{1})
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);

        isolation.close();
    }

    @Test
    void rejectsEmptyBody(@TempDir Path root) {

        Library        library   = library(root);
        UUID           episodeId = UUID.randomUUID();
        EpisodeService episodes  = mock(EpisodeService.class);
        when(episodes.requireById(episodeId)).thenReturn(episode(episodeId));

        IsolationSession isolation = library.createIsolation(
                Set.of(new AccessScope(Library.EPISODES, episodeId.toString()))
        );

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library, episodes).stageEpisode(
                        episodeId,
                        isolation,
                        new ByteArrayInputStream(new byte[0])
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);

        isolation.close();
    }

    @Test
    void rejectsMissingBody(@TempDir Path root) {

        Library        library   = library(root);
        UUID           episodeId = UUID.randomUUID();
        EpisodeService episodes  = mock(EpisodeService.class);
        when(episodes.requireById(episodeId)).thenReturn(episode(episodeId));

        IsolationSession isolation = library.createIsolation(
                Set.of(new AccessScope(Library.EPISODES, episodeId.toString()))
        );

        WebException ex = assertThrows(
                WebException.class,
                () -> controller(library, episodes).stageEpisode(episodeId, isolation, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);

        isolation.close();
    }
}
