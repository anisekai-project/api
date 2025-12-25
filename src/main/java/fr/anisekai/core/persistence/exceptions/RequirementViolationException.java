package fr.anisekai.core.persistence.exceptions;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception used by {@link AnisekaiRepository} and its implementations when one of the {@code require} method fails to
 * fetch an entity.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RequirementViolationException extends RuntimeException {

    /**
     * Create a new instance of this exception.
     */
    public RequirementViolationException() {

        super("Failed to fetch entity when required.");
    }

}
