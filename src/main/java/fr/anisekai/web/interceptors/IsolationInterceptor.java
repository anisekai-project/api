package fr.anisekai.web.interceptors;

import fr.anisekai.library.Library;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.annotations.RequireIsolation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.UUID;

@Component
public class IsolationInterceptor implements HandlerInterceptor {


    private static final Logger LOGGER = LoggerFactory.getLogger(IsolationInterceptor.class);

    private final Library library;

    public IsolationInterceptor(Library library) {

        this.library = library;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true; // Not a controller method — allow
        }

        if (!method.hasMethodAnnotation(RequireIsolation.class)) {
            return true; // Does not have an isolation requirement.
        }

        String route = String.format("%s %s", request.getMethod(), request.getRequestURI());

        if (!method.hasMethodAnnotation(RequireAuth.class)) {
            LOGGER.error("[{}] <!> @RequireIsolation used without @RequireAuth", route);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Misconfiguration");
            return false;
        }

        SessionToken sessionToken = (SessionToken) request.getAttribute("session");

        // Previous interceptor (authentication) should have handled this, but better safe than sorry.
        if (sessionToken == null) {
            LOGGER.warn("[{}] Can't provide isolation context: No session available", route);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "IsolationSession not available right now.");
            return false;
        }

        String isolation = request.getHeader("X-Isolation-Context");

        if (isolation == null) {
            LOGGER.warn("[{}] Can't provide isolation context: No session available", route);
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Unable to read the isolation context id from headers"
            );
            return false;
        }

        UUID uuid;

        try {
            uuid = UUID.fromString(isolation);
        } catch (Exception e) {
            LOGGER.warn("[{}] Can't provide isolation context: Unable to parse isolation context id", route);
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Unable to parse the isolation context id from headers"
            );
            return false;
        }

        Optional<IsolationSession> optionalIsolation = this.library.resolveIsolation(sessionToken, uuid);

        if (optionalIsolation.isEmpty()) {
            LOGGER.warn("[{}] Can't provide isolation context: No matching isolation", route);
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "The provided isolation id does not match any allowed isolation session."
            );
            return false;
        }

        request.setAttribute("isolation", optionalIsolation.get());
        return true;
    }

}
