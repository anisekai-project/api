package fr.anisekai.core.internal.sentry;

import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.TransactionOptions;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Implementation of {@link ITimedAction} when Sentry is enabled.
 */
public class TimedAction implements ITimedAction {

    private final BlockingDeque<ISpan> span;
    private       ITransaction         transaction;

    TimedAction() {

        this.span = new LinkedBlockingDeque<>();
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
    @Override
    public void open(String transaction, String name, String description) {

        TransactionOptions options = new TransactionOptions();
        options.setBindToScope(true);
        this.transaction = Sentry.startTransaction(transaction, name, options);
        this.transaction.setDescription(description);
    }

    /**
     * Close all opened transaction and send the timing information to Sentry.
     */
    @Override
    public void close() {

        while (!this.span.isEmpty()) {
            ISpan span = this.span.pollLast();
            if (span != null) {
                span.finish();
            }
        }
        this.transaction.finish();
    }

    /**
     * Start an action, opening a sub-transaction withing the actual timing. Calling this multiple time will open
     * another sub-transaction (inside the current sub-transaction). This can be repeated an unlimited amount of time.
     *
     * @param name
     *         The name of the action that will be executed.
     * @param description
     *         The description of the action that will be executed.
     */
    @Override
    public void action(String name, String description) {

        if (this.span.isEmpty()) {
            this.span.offer(this.transaction.startChild(name, description));
            return;
        }

        this.span.offer(this.span.getLast().startChild(name, description));
    }

    /**
     * Stop the last opened action, causing its transaction to be closed.
     */
    @Override
    public void endAction() {

        if (this.span.isEmpty()) {
            return;
        }

        ISpan span = this.span.pollLast();
        if (span != null) {
            span.finish();
        }
    }


}
