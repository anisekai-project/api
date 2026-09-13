package fr.anisekai.web.interceptors;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.enums.TokenType;
import fr.anisekai.web.exceptions.auth.AuthenticationMissingException;
import fr.anisekai.web.exceptions.auth.MalformedAuthorizationException;
import fr.anisekai.web.exceptions.auth.RouteAccessDeniedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger                LOGGER = LoggerFactory.getLogger(AuthenticationInterceptor.class);
    private final        AuthenticationManager manager;

    public AuthenticationInterceptor(AuthenticationManager manager, ApplicationConfiguration configuration) {

        this.manager = manager;
    }

    private Optional<String> getTokenFromCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        String accessToken = null;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("anisekai-access-token")) {
                accessToken = cookie.getValue();
            }
        }

        return Optional.ofNullable(accessToken);
    }

    private Optional<String> getTokenFromAuthorization(String route, HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null) {
            return Optional.empty();
        }

        if (!authHeader.toLowerCase().startsWith("bearer ")) {
            LOGGER.trace("[{}] Denied access: Invalid authorization header.", route);
            throw new MalformedAuthorizationException();
        }

        return Optional.of(authHeader.substring(7).trim());
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {

        if (!(handler instanceof HandlerMethod method)) {
            return true; // Not a controller method — allow
        }

        String    route = String.format("%s %s", request.getMethod(), request.getRequestURI());
        RouteRule rule  = new RouteRule(method.getMethodAnnotation(RequireAuth.class));

        if (!rule.isAuthRequired()) {
            LOGGER.trace("[{}] Allowed anonymous access.", route);
            return true; // No @RequireAuth — allow
        }


        String accessToken = this.getTokenFromCookie(request)
                                 .or(() -> this.getTokenFromAuthorization(route, request))
                                 .orElseThrow(AuthenticationMissingException::new);

        UUID         uuid    = this.manager.getJti(accessToken);
        SessionToken session = this.manager.getAccessToken(uuid);

        if (!rule.canAccess(session)) {
            LOGGER.info("[{}] ({}) Denied access: Rules mismatch.", route, session.getOwner().getId());
            throw new RouteAccessDeniedException(route, session.getOwner());
        }

        // Optionally store session for later use
        request.setAttribute("session", session);
        LOGGER.trace("[{}] ({}) Accessed secured resource.", route, session.getOwner().getId());
        return true; // Allow
    }

    /**
     * Represents the authentication and authorization policy of a route based on the {@link RequireAuth} annotation.
     */
    private static class RouteRule {

        private final boolean            authRequired;
        private       boolean            adminRequired     = false;
        private       boolean            guestsAllowed     = true;
        private       EnumSet<TokenType> tokenTypesAllowed = EnumSet.of(TokenType.USER, TokenType.APPLICATION);
        private       Set<String>        requiredScopes    = Set.of();

        public RouteRule(@Nullable RequireAuth auth) {

            this.authRequired = auth != null;

            if (this.authRequired) {
                this.adminRequired     = auth.requireAdmin();
                this.guestsAllowed     = auth.allowGuests();
                this.tokenTypesAllowed = EnumSet.noneOf(TokenType.class);
                Collections.addAll(this.tokenTypesAllowed, auth.allowedSessionTypes());
                this.requiredScopes = Set.of(auth.scopes());
            }
        }

        /**
         * Check the {@code APPLICATION} token scopes required by the route.
         * <p>
         * TRANSITIONAL: {@code USER} sessions bypass scope checks. {@code USER} tokens will disappear once every token
         * is an {@code APPLICATION} token (possibly behind an OpenID-compliant solution); remove this bypass then.
         *
         * @param sessionToken
         *         The resolved session.
         *
         * @return {@code true} when no scope is required or every required scope is granted.
         */
        static boolean hasRequiredScopes(SessionToken sessionToken, Collection<String> requiredScopes) {

            if (requiredScopes == null || requiredScopes.isEmpty()) {
                return true;
            }
            if (sessionToken.getType() == TokenType.USER) {
                return true;
            }
            Set<String> granted = sessionToken.getScopes();
            return granted != null && granted.containsAll(requiredScopes);
        }

        public boolean isAuthRequired() {

            return this.authRequired;
        }

        public boolean canAccess(SessionToken sessionToken) {

            DiscordUser user = sessionToken.getOwner();

            boolean guestCheckPass = !user.isGuest() || this.guestsAllowed;
            boolean adminCheckPass = user.isAdministrator() || !this.adminRequired;
            boolean typeCheckPass  = this.tokenTypesAllowed.contains(sessionToken.getType());
            boolean scopeCheckPass = this.hasRequiredScopes(sessionToken);

            return guestCheckPass && adminCheckPass && typeCheckPass && scopeCheckPass;
        }

        private boolean hasRequiredScopes(SessionToken sessionToken) {

            return hasRequiredScopes(sessionToken, this.requiredScopes);
        }

    }

}
