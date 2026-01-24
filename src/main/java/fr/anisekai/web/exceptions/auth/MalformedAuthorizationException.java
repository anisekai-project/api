package fr.anisekai.web.exceptions.auth;

import fr.anisekai.web.exceptions.WebException;
import org.springframework.http.HttpStatus;

public class MalformedAuthorizationException extends WebException {

    public MalformedAuthorizationException() {

        super(
                HttpStatus.UNAUTHORIZED,
                "Authorization header not a bearer token",
                "Authorization header not a bearer token"
        );
    }

}
