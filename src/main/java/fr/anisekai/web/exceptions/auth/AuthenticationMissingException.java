package fr.anisekai.web.exceptions.auth;

import fr.anisekai.web.exceptions.WebException;
import org.springframework.http.HttpStatus;

public class AuthenticationMissingException extends WebException {

    public AuthenticationMissingException() {

        super(
                HttpStatus.UNAUTHORIZED,
                "Missing cookie 'anisekai-access-token' or authorization bearer",
                "Missing cookie 'anisekai-access-token' or authorization bearer"
        );
    }

}
