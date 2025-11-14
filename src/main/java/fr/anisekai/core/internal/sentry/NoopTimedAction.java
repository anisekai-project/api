package fr.anisekai.core.internal.sentry;


/**
 * Implementation of {@link ITimedAction} when Sentry is disabled.
 */
public class NoopTimedAction implements ITimedAction {

    /**
     * Constructs a new no-op timed action.
     */
    public NoopTimedAction() {}

    @Override
    public void open(String transaction, String name, String description) {}

    @Override
    public void action(String name, String description) {}

    @Override
    public void endAction() {}

    @Override
    public void close() {}

}
