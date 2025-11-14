package fr.anisekai.core.internal.plannifier.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when a scheduled event cannot be successfully persisted by the underlying service.
 * <p>
 * This exception typically indicates that the scheduler successfully created the event in memory, but the persistence
 * layer (e.g., database or external service) rejected or failed to save it.
 */
public class SchedulingCreationException extends RuntimeException implements FriendlyException {

    /**
     * Create a new exception with a default technical message and wraps the underlying cause.
     *
     * @param e
     *         The cause of the failure, typically an exception from the persistence layer.
     */
    public SchedulingCreationException(Throwable e) {

        super("The parent service managing scheduled event denied or was unable to save the new event.", e);
    }

    @Override
    public @NotNull String getFriendlyMessage() {

        return "L'évènement a été créé mais sa sauvegarde a échouée.";
    }

    @Override
    public boolean mayBePublic() {

        return true;
    }

}
