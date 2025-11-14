package fr.anisekai.core.internal.plannifier.exceptions;


import fr.anisekai.core.internal.plannifier.interfaces.entities.Planifiable;

/**
 * Exception thrown when attempting to delay {@link Planifiable} in a way that would cause overlap with existing
 * {@link Planifiable}.
 * <p>
 * Implements {@link FriendlyException} to provide a user-facing error message suitable for UI display.
 */
public class DelayOverlapException extends RuntimeException implements FriendlyException {

    /**
     * Creates a new {@code DelayOverlapException} with the specified detail message and cause.
     *
     * @param message
     *         The detail message describing the cause of the exception
     * @param cause
     *         The underlying cause of the exception
     */

    public DelayOverlapException(String message, Throwable cause) {

        super(message, cause);
    }

    /**
     * Creates a new {@code DelayOverlapException} with the specified detail message.
     *
     * @param message
     *         The detail message describing the cause of the exception
     */

    public DelayOverlapException(String message) {

        super(message);
    }

    @Override
    public String getFriendlyMessage() {

        return "Impossible de décaler les évènements: Cela entrerait en conflit avec des séances déjà programmée.";
    }

    @Override
    public boolean mayBePublic() {

        return true;
    }

}
