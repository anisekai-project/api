package fr.anisekai.web.configs;

import fr.anisekai.web.exceptions.WebException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WebAdvice {

    public static final boolean DETAILS_ENABLED = false;

    @ExceptionHandler(WebException.class)
    public ResponseEntity<?> handle(WebException ex) {

        return ResponseEntity
                .status(ex.status)
                .body(DETAILS_ENABLED ? ex : WebException.Dto.from(ex, DETAILS_ENABLED));
    }

}
