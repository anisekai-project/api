package fr.anisekai.web.exceptions.auth;

import fr.anisekai.web.exceptions.WebException;
import org.springframework.http.HttpStatus;

public class CookiesMissingException extends WebException {

    public CookiesMissingException() {

        super(
                HttpStatus.UNAUTHORIZED,
                "Missing authentication cookies",
                "Missing authentication cookies"
        );
    }

}
