package fr.anisekai.web.exceptions.auth;

import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.web.exceptions.WebException;
import org.springframework.http.HttpStatus;

public class RouteAccessDeniedException extends WebException {

    public RouteAccessDeniedException(String route, DiscordUser user) {

        super(
                HttpStatus.FORBIDDEN,
                "Access to route [%s] for user [%s] has been denied.".formatted(route, user.getId()),
                "Access to resource forbidden."
        );
    }

}
