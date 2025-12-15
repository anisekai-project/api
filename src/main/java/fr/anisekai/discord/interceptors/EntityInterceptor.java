package fr.anisekai.discord.interceptors;

import fr.alexpado.interactions.interfaces.routing.Interceptor;
import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.structure.Endpoint;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.services.UserService;
import net.dv8tion.jda.api.entities.User;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(2)
public class EntityInterceptor implements Interceptor {

    private final UserService userService;

    public EntityInterceptor(UserService userService) {

        this.userService = userService;
    }

    @Override
    public Optional<Object> preHandle(@NonNull Endpoint<?> endpoint, @NonNull Request<?> request) {

        User        user    = request.getEvent().getUser();
        DiscordUser appUser = this.userService.of(user);
        request.addAttachment(DiscordUser.class, appUser);

        return Optional.empty();
    }

}
