package fr.anisekai.server.domain.entities;

import fr.anisekai.server.repositories.SessionTokenRepository;
import fr.anisekai.web.enums.TokenType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class SessionTokenScopePersistenceTest {

    @Autowired
    private SessionTokenRepository tokens;

    @Autowired
    private TestEntityManager entities;

    @Test
    void scopesSurviveRoundTrip() {

        DiscordUser owner = new DiscordUser();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setNickname("owner");
        owner.setGuest(false);
        this.entities.persist(owner);

        SessionToken token = new SessionToken();
        token.setId(UUID.randomUUID());
        token.setOwner(owner);
        token.setType(TokenType.APPLICATION);
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setScopes(Set.of("worker", "anime.write"));
        this.tokens.save(token);
        this.entities.flush();
        this.entities.clear();

        SessionToken reloaded = this.tokens.findById(token.getId()).orElseThrow();
        assertEquals(Set.of("worker", "anime.write"), reloaded.getScopes());
    }

    @Test
    void tokensWithoutScopesPersistEmpty() {

        DiscordUser owner = new DiscordUser();
        owner.setId(2L);
        owner.setUsername("legacy");
        owner.setNickname("legacy");
        owner.setGuest(false);
        this.entities.persist(owner);

        SessionToken token = new SessionToken();
        token.setId(UUID.randomUUID());
        token.setOwner(owner);
        token.setType(TokenType.APPLICATION);
        token.setExpiresAt(Instant.now().plusSeconds(60));
        this.tokens.save(token);
        this.entities.flush();
        this.entities.clear();

        SessionToken reloaded = this.tokens.findById(token.getId()).orElseThrow();
        assertTrue(reloaded.getScopes().isEmpty());
    }

}
