package fr.anisekai.discord.interceptors;

import fr.alexpado.interactions.interfaces.routing.Interceptor;
import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.providers.ReflectiveRouteHandler;
import fr.alexpado.interactions.structure.Endpoint;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.domain.entities.DiscordUser;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;

@Component
@Order(3)
public class AdminInterceptor implements Interceptor {

    @Override
    public Optional<Object> preHandle(@NonNull Endpoint<?> endpoint, @NonNull Request<?> request) {

        if (endpoint.handler() instanceof ReflectiveRouteHandler<?> handler) {

            Method   method = handler.getMethod();
            Class<?> clazz  = handler.getMethod().getDeclaringClass();

            boolean methodRequireAdmin = method.isAnnotationPresent(RequireAdmin.class);
            boolean clazzRequireAdmin  = clazz.isAnnotationPresent(RequireAdmin.class);

            if (methodRequireAdmin || clazzRequireAdmin) {
                DiscordUser attachment = request.getAttachment(DiscordUser.class);

                if (attachment == null) {
                    return Optional.of(DiscordResponse.error("Une erreur est survenue. (err: usr_null)"));
                }

                if (!attachment.isAdministrator()) {
                    return Optional.of(DiscordResponse.error("Tu n'as pas les droits de faire ceci."));
                }
            }
        }

        return Optional.empty();
    }

}
