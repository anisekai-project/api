package fr.anisekai.server.tasking;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.server.domain.entities.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Marker for server factories whose tasks require an isolation context staging area
 * when attributed to a worker.
 * <p>
 * The scopes must be a pure function of the task and its input: the orchestrator
 * creates the session and persists its identifier on the task.
 *
 * @param <I>
 *         The input argument type.
 */
public interface IsolatedServerFactory<I> {

    /**
     * Compute the access scopes the attributed isolation context must grant.
     *
     * @param task
     *         The claimed task.
     * @param input
     *         The deserialized task input.
     *
     * @return The required scopes, never {@code null}.
     */
    @NotNull Set<AccessScope> getIsolationScopes(@NotNull Task task, @NotNull I input);
}
