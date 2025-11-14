package fr.anisekai.web.interceptors;

import fr.anisekai.core.persistence.AnisekaiServiceRegistry;
import fr.anisekai.core.persistence.interfaces.CloseableContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PersistenceInterceptor implements HandlerInterceptor {

    private final static Logger LOGGER = LoggerFactory.getLogger(PersistenceInterceptor.class);

    private final        AnisekaiServiceRegistry registry;
    private static final String                  EVENT_CONTEXT_ATTRIBUTE = "anisekai.internal.events";

    public PersistenceInterceptor(AnisekaiServiceRegistry registry) {

        this.registry = registry;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {

        CloseableContext context = this.registry.openEventContexts();
        request.setAttribute(EVENT_CONTEXT_ATTRIBUTE, context);
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {

        if (request.getAttribute(EVENT_CONTEXT_ATTRIBUTE) instanceof CloseableContext context) {
            try {
                context.close();
            } catch (Exception e) {
                LOGGER.error("Unable to close event contexts", e);
            }
        }
    }

}
