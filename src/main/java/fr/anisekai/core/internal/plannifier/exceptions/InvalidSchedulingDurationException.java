package fr.anisekai.core.internal.plannifier.exceptions;


import fr.anisekai.core.internal.plannifier.interfaces.entities.Planifiable;

/**
 * Exception thrown when attempting to schedule a {@link Planifiable} with an invalid or non-positive duration.
 * <p>
 * This exception is considered safe to expose to end users.
 */
public class InvalidSchedulingDurationException extends RuntimeException implements FriendlyException {

    /**
     * Constructs a new {@link InvalidSchedulingDurationException} with a default error message.
     */
    public InvalidSchedulingDurationException() {

        super("Unable to schedule an event with an invalid duration.");
    }

    @Override
    public String getFriendlyMessage() {

        return "Impossible de planifier un évènement avec une durée invalide.";
    }

    @Override
    public boolean mayBePublic() {

        return true;
    }

}
