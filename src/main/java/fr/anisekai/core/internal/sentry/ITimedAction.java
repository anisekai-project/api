package fr.anisekai.core.internal.sentry;

import io.sentry.Sentry;

/**
 * Interface representing an object able to send performance statistics to Sentry.
 */
public interface ITimedAction extends AutoCloseable {

    /**
     * Creates and returns an {@link ITimedAction} implementation. The implementation will differ depending on the
     * Sentry state. If Sentry is enabled, a {@link TimedAction} will be returned, or else a {@link NoopTimedAction}
     * will be returned.
     *
     * @return An {@link ITimedAction} implementation.
     */
    static ITimedAction create() {

        return Sentry.isEnabled() ? new TimedAction() : new NoopTimedAction();
    }

    /**
     * Start the timing transaction of the current {@link ITimedAction}.
     *
     * @param transaction
     *         The transaction name
     * @param name
     *         The name of this timing transaction.
     * @param description
     *         The description of this timing transaction.
     */
    void open(String transaction, String name, String description);

    /**
     * Start an action, opening a sub-transaction withing the actual timing. Calling this multiple time will open
     * another sub-transaction (inside the current sub-transaction). This can be repeated an unlimited amount of time.
     *
     * @param name
     *         The name of the action that will be executed.
     * @param description
     *         The description of the action that will be executed.
     */
    void action(String name, String description);

    /**
     * Stop the last opened action, causing its transaction to be closed.
     */
    void endAction();

    /**
     * Close all opened transaction and send the timing information to Sentry.
     */
    @Override
    void close();

}
