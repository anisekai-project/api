package fr.anisekai.discord.interceptors;

import fr.alexpado.interactions.interfaces.routing.Interceptor;
import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.structure.Endpoint;
import fr.anisekai.core.persistence.AnisekaiServiceRegistry;
import fr.anisekai.core.persistence.interfaces.CloseableContext;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(1)
public class EventContextInterceptor implements Interceptor {

    private final AnisekaiServiceRegistry registry;

    public EventContextInterceptor(AnisekaiServiceRegistry registry) {

        this.registry = registry;
    }

    @Override
    public Optional<Object> preHandle(@NonNull Endpoint<?> endpoint, @NotNull Request<?> request) {

        String  scheme         = endpoint.route().getUri().getScheme();
        boolean isActiveScheme = "slash".equals(scheme) || "button".equals(scheme);

        if (isActiveScheme) {
            CloseableContext context = this.registry.openEventContexts();
            request.addAttachment(CloseableContext.class, context);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Object> postHandle(@NonNull Endpoint<?> endpoint, @NotNull Request<?> request, @NotNull Object result) {

        CloseableContext attachment = request.getAttachment(CloseableContext.class);
        if (attachment != null) {
            attachment.close();
        }

        return Optional.empty();
    }

}
