package fr.anisekai.core.internal.plannifier.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when an existing scheduled event could not be updated or saved by the persistence layer.
 * <p>
 * This typically means that the modification request was valid, but the backend service (e.g., database) failed to
 * persist the changes.
 */
public class SchedulingUpdatingException extends RuntimeException implements FriendlyException {

    /**
     * Constructs a new exception indicating that the update operation failed, wrapping the underlying cause of the
     * persistence failure.
     *
     * @param e
     *         the root cause of the failure, usually thrown by the underlying service
     */
    public SchedulingUpdatingException(Throwable e) {

        super("The parent service managing scheduled events was unable to save the event.", e);
    }

    @Override
    public @NotNull String getFriendlyMessage() {

        return "La sauvegarde d'un évènement a échouée.";
    }

    @Override
    public boolean mayBePublic() {

        return true;
    }

}
