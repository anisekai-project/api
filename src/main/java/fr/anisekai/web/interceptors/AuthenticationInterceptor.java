package fr.anisekai.web.interceptors;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.enums.TokenType;
import fr.anisekai.web.exceptions.auth.AuthorizationHeadersMissingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    /**
     * Represents the authentication and authorization policy of a route based on the {@link RequireAuth} annotation.
     */
    private static class RouteRule {

        private final boolean            authRequired;
        private       boolean            adminRequired     = false;
        private       boolean            guestsAllowed     = true;
        private       EnumSet<TokenType> tokenTypesAllowed = EnumSet.of(TokenType.USER, TokenType.APPLICATION);

        public RouteRule(@Nullable RequireAuth auth) {

            this.authRequired = auth != null;

            if (this.authRequired) {
                this.adminRequired     = auth.requireAdmin();
                this.guestsAllowed     = auth.allowGuests();
                this.tokenTypesAllowed = EnumSet.noneOf(TokenType.class);
                Collections.addAll(this.tokenTypesAllowed, auth.allowedSessionTypes());
            }
        }

        public boolean isAuthRequired() {

            return this.authRequired;
        }

        public boolean canAccess(SessionToken sessionToken) {

            DiscordUser user = sessionToken.getOwner();

            boolean guestCheckPass = !user.isGuest() || this.guestsAllowed;
            boolean adminCheckPass = user.isAdministrator() || !this.adminRequired;
            boolean typeCheckPass  = this.tokenTypesAllowed.contains(sessionToken.getType());

            return guestCheckPass && adminCheckPass && typeCheckPass;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    private final AuthenticationManager manager;

    public AuthenticationInterceptor(AuthenticationManager manager, ApplicationConfiguration configuration) {

        this.manager = manager;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true; // Not a controller method — allow
        }

        String    route = String.format("%s %s", request.getMethod(), request.getRequestURI());
        RouteRule rule  = new RouteRule(method.getMethodAnnotation(RequireAuth.class));

        if (!rule.isAuthRequired()) {
            LOGGER.trace("[{}] Allowed anonymous access.", route);
            return true; // No @RequireAuth — allow
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            LOGGER.trace("[{}] Denied access: No bearer provided", route);
            throw new AuthorizationHeadersMissingException();
        }

        String       accessToken = authHeader.substring(7).trim();
        UUID         uuid        = this.manager.getJti(accessToken);
        SessionToken session     = this.manager.getAccessToken(uuid);

        if (!rule.canAccess(session)) {
            LOGGER.info("[{}] ({}) Denied access: Rules mismatch.", route, session.getOwner().getId());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return false;
        }

        // Optionally store session for later use
        request.setAttribute("session", session);
        LOGGER.trace("[{}] ({}) Accessed secured resource.", route, session.getOwner().getId());
        return true; // Allow
    }

}
