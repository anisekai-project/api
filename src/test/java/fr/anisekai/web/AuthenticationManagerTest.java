package fr.anisekai.web;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.repositories.SessionTokenRepository;
import fr.anisekai.server.services.UserService;
import fr.anisekai.web.enums.TokenScope;
import fr.anisekai.web.enums.TokenType;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationManagerTest {

    private record Fixture(AuthenticationManager manager, SecretKey key) {

    }

    @Test
    void scopedApplicationTokenPersistsScopes() {

        Fixture fixture = fixture();
        DiscordUser owner = owner();

        SessionToken token = fixture.manager().createApplicationToken(owner, Instant.now().plusSeconds(60), List.of(TokenScope.WORKER));

        assertTrue(token.getScopes().contains(TokenScope.WORKER));
    }

    @Test
    void unknownScopeIsRejectedAtMint() {

        Fixture fixture = fixture();

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.manager().createApplicationToken(owner(), Instant.now().plusSeconds(60), List.of("anime.read"))
        );
    }

    @Test
    void stringifyEmitsSpaceDelimitedScopesClaim() {

        Fixture fixture = fixture();
        SessionToken token = token(TokenScope.WORKER, TokenScope.ANIME_WRITE);

        String jwt = fixture.manager().stringify(token);
        Object scp = Jwts.parser()
                         .verifyWith(fixture.key())
                         .build()
                         .parseSignedClaims(jwt)
                         .getPayload()
                         .get("scp");

        assertEquals("anime.write worker", scp);
    }

    @Test
    void stringifyOmitsScopesClaimWhenEmpty() {

        Fixture fixture = fixture();
        SessionToken token = token();

        String jwt = fixture.manager().stringify(token);
        Object scp = Jwts.parser()
                         .verifyWith(fixture.key())
                         .build()
                         .parseSignedClaims(jwt)
                         .getPayload()
                         .get("scp");

        assertNull(scp);
    }

    private static Fixture fixture() {

        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");

        ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.getApi().setSigningKey(Base64.getEncoder().encodeToString(secret));

        SessionTokenRepository repository = mock(SessionTokenRepository.class);
        when(repository.save(any(SessionToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        return new Fixture(new AuthenticationManager(configuration, mock(UserService.class), repository), key);
    }

    private static DiscordUser owner() {

        DiscordUser owner = new DiscordUser();
        owner.setId(1L);
        return owner;
    }

    private static SessionToken token(String... scopes) {

        Instant now = Instant.now();

        SessionToken token = new SessionToken();
        token.setId(UUID.randomUUID());
        token.setOwner(owner());
        token.setType(TokenType.APPLICATION);
        token.setExpiresAt(now.plusSeconds(60));
        token.setScopes(scopes.length == 0 ? Set.of() : new LinkedHashSet<>(List.of(scopes)));
        ReflectionTestUtils.setField(token, "createdAt", now);
        ReflectionTestUtils.setField(token, "updatedAt", now);
        return token;
    }

}
