package fr.anisekai.server.tasking;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.validation.JsonObjectRule;
import fr.anisekai.core.internal.sentry.ITimedAction;

public interface TaskExecutor {

    String OPTION_PRIORITY = "priority";

    /**
     * Check if the executor can find the required content in the provide {@link AnisekaiJson} for its execution.
     *
     * @param params
     *         A {@link AnisekaiJson}
     */
    default void validateParams(AnisekaiJson params) {

        params.validate(
                new JsonObjectRule(OPTION_PRIORITY, true, Integer.class, int.class)
        );
    }

    /**
     * Run this task.
     *
     * @param timer
     *         The timer to use to mesure performance of the task.
     * @param params
     *         The parameters of this task.
     *
     * @throws Exception
     *         Thew if something happens.
     */
    void execute(ITimedAction timer, AnisekaiJson params) throws Exception;

}
