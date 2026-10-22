package fr.anisekai.web.api;

import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.core.persistence.UpsertResult;
import fr.anisekai.core.persistence.UpsertAction;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.web.dto.AnimeImportRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnimeControllerTest {

    @Test
    void importAnimeReturnsSuccess() {

        AnimeService service = mock(AnimeService.class);
        DiscordUser user = new DiscordUser();
        user.setId(1L);
        SessionToken token = new SessionToken();
        token.setOwner(user);
        Anime anime = new Anime(user);
        anime.setId(UUID.randomUUID());

        UpsertResult<Anime> result = new UpsertResult<>(anime, UpsertAction.INSERTED);
        when(service.importAnime(any(), any())).thenReturn(result);

        AnimeController controller = new AnimeController(service);
        AnimeImportRequest request = new AnimeImportRequest("Test Anime", "WATCHING", "https://example.com/anime", "https://example.com/img.jpg", 10, 24, "Test Group", null, null, null, (byte) 1);
        String response = controller.importAnime(token, request);

        assertTrue(response.contains("\"result.success\":true"));
        assertTrue(response.contains("\"result.state\":\"INSERTED\""));
    }
}
