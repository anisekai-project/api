package fr.anisekai.server.tasking;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.enums.StoreType;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.Task;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
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

    /**
     * Validate the staged output before the isolation context is committed on task success.
     * <p>
     * The default implementation requires every granted scope to resolve to an existing path
     * ({@code FILE_SCOPED} scopes must be regular files). Directory scopes may legitimately be
     * empty (e.g. no subtitle tracks), so only existence is checked.
     *
     * @param isolation
     *         The isolation context holding the staged output.
     * @param task
     *         The succeeding task.
     * @param input
     *         The deserialized task input.
     *
     * @throws IllegalStateException
     *         when the staged output is missing or unusable.
     */
    default void validateStagedOutput(@NotNull IsolationSession isolation, @NotNull Task task, @NotNull I input) {

        for (AccessScope scope : this.getIsolationScopes(task, input)) {
            Path staged = isolation.resolve(scope);
            if (scope.store().type() == StoreType.FILE_SCOPED) {
                if (!Files.isRegularFile(staged)) {
                    throw new IllegalStateException("Staged file is missing: " + scope);
                }
            } else if (!Files.exists(staged)) {
                throw new IllegalStateException("Staged directory is missing: " + scope);
            }
        }
    }
}
