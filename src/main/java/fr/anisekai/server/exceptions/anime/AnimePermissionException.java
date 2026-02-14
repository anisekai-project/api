package fr.anisekai.server.exceptions.anime;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AnimePermissionException extends RuntimeException {

    public AnimePermissionException(String message) {

        super(message);
    }

}
