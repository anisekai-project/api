package fr.anisekai.core.persistence.exceptions;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception used by {@link AnisekaiRepository} and its implementations when one of the {@code forbid} method succeed to
 * fetch an entity.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ForbiddenViolationException extends RuntimeException {

    /**
     * Create a new instance of this exception.
     */
    public ForbiddenViolationException() {

        super("Succeeded to fetch entity when forbidden.");
    }

}
