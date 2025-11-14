package fr.anisekai.core.internal.plannifier.exceptions;

/**
 * Represents an exception that provides a user-friendly message suitable for display in a user interface.
 * <p>
 * Useful for distinguishing between internal error messages and messages that can be safely shown to end users.
 */
public interface FriendlyException {

    /**
     * Retrieve the friendly message for the exception. The friendly message is just the message to display to the
     * user.
     *
     * @return A friendly message.
     */
    String getFriendlyMessage();

    /**
     * Check if this exception can be publicly displayed or if it should stay private.
     *
     * @return True if the message can be public, false otherwise.
     */
    boolean mayBePublic();

}
