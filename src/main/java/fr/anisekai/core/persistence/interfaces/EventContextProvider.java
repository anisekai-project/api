package fr.anisekai.core.persistence.interfaces;

/**
 * Represent an object having an event context ability.
 */
public interface EventContextProvider {

    /**
     * Open an event context and returns the {@link CloseableContext} resource that can close that event context.
     *
     * @return A {@link CloseableContext} resource to close when the event context can be dropped.
     */
    CloseableContext startEventContext();

}
