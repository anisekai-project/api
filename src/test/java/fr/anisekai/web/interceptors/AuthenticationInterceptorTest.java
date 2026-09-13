package fr.anisekai.web.interceptors;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.enums.TokenScope;
import fr.anisekai.web.enums.TokenType;
import fr.anisekai.web.exceptions.auth.RouteAccessDeniedException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationInterceptorTest {

    static class ScopedController {

        @RequireAuth(allowedSessionTypes = {TokenType.USER, TokenType.APPLICATION}, scopes = TokenScope.WORKER)
        public void worker() {

        }

        @RequireAuth(allowedSessionTypes = TokenType.APPLICATION)
        public void unscoped() {

        }

    }

    @Test
    void scopedApplicationTokenPasses() throws Exception {

        assertTrue(preHandle("worker", applicationToken(Set.of(TokenScope.WORKER))));
    }

    @Test
    void scopelessApplicationTokenIsDeniedOnScopedRoute() {

        SessionToken token = applicationToken(Set.of());

        assertThrows(RouteAccessDeniedException.class, () -> preHandle("worker", token));
    }

    @Test
    void wrongScopeIsDeniedOnScopedRoute() {

        SessionToken token = applicationToken(Set.of(TokenScope.ANIME_WRITE));

        assertThrows(RouteAccessDeniedException.class, () -> preHandle("worker", token));
    }

    @Test
    void userTokenBypassesScopeCheckTransitionally() throws Exception {

        SessionToken token = applicationToken(Set.of());
        token.setType(TokenType.USER);

        assertTrue(preHandle("worker", token));
    }

    @Test
    void scopelessApplicationTokenPassesOnUnscopedRoute() throws Exception {

        assertTrue(preHandle("unscoped", applicationToken(Set.of())));
    }

    private static boolean preHandle(String methodName, SessionToken token) throws Exception {

        UUID jwtId = UUID.randomUUID();
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.getJti("jwt")).thenReturn(jwtId);
        when(manager.getAccessToken(jwtId)).thenReturn(token);

        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(manager, mock(ApplicationConfiguration.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v3/test");

        Method handler = ScopedController.class.getMethod(methodName);

        return interceptor.preHandle(request, mock(HttpServletResponse.class), new HandlerMethod(new ScopedController(), handler));
    }

    private static SessionToken applicationToken(Set<String> scopes) {

        DiscordUser owner = new DiscordUser();
        owner.setId(1L);
        owner.setGuest(false);
        owner.setAdministrator(false);

        SessionToken token = new SessionToken();
        token.setId(UUID.randomUUID());
        token.setOwner(owner);
        token.setType(TokenType.APPLICATION);
        token.setScopes(scopes);
        return token;
    }

}
