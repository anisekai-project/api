package fr.anisekai.core.persistence.interfaces;

/**
 * Represents a context that is active within a try-with-resources block.
 * <p>
 * This interface extends {@link AutoCloseable} but guarantees that its {@link #close()} method will not throw a checked
 * exception, removing the need for a catch clause.
 */
public interface CloseableContext extends AutoCloseable {

    /**
     * Closes this context and releases any resources associated with it for the current scope. This implementation does
     * not throw a checked exception.
     */
    @Override
    void close();

}
