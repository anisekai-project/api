package fr.anisekai.server.tasking;

import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.parser.Entity;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface TaskFactory<T extends TaskExecutor> {

    /**
     * Get this {@link TaskFactory} name, which will be used to associate it with a {@link Task}.
     *
     * @return The {@link TaskFactory} name.
     */
    @NotNull String getName();

    /**
     * Create an instance of {@link TaskExecutor}. This is useful if your task has some bean dependencies.
     *
     * @return A new {@link TaskExecutor} instance.
     */
    @NotNull T create();

    /**
     * Check if this {@link TaskFactory} allows multiple {@link Task} with the same {@link Task#getName()}.
     *
     * @return True if duplicates are allowed, false otherwise.
     */
    default boolean allowDuplicated() {

        return !this.hasNamedTask();
    }

    /**
     * Check if this {@link TaskFactory} has named tasks. Named tasks usually mean that each {@link TaskExecutor}
     * created is associated to a specific {@link Entity} and thus will have a specific name for each of them.
     *
     * @return True if this {@link TaskFactory} handles named task, false otherwise.
     */
    boolean hasNamedTask();

    /**
     * Determines if tasks from this factory can be executed by external workers.
     *
     * @return {@code true} if the tasks are public, {@code false} otherwise.
     */
    default boolean isPublic() {

        return false;
    }

    /**
     * Gets the default lease duration for {@link Task}s created by this {@link TaskFactory}. When a {@link Worker}
     * acquires a {@link Task}, this duration determines how long the {@link Worker} has to complete it or send a
     * heartbeat before the {@link Task} is considered expired and can be rescheduled.
     *
     * @return The lease duration. Defaults to 1 hour.
     */
    default Duration getLeaseDuration() {

        return Duration.ofHours(1);
    }

    /**
     * Defines the set of {@link AccessScope}s required for a task's isolation session. These scopes represent the
     * directories the task will be able to write its final output to.
     *
     * @param task
     *         The task for which to determine scopes.
     *
     * @return A set of required access scopes.
     */
    default Set<AccessScope> getRequiredScopes(Task task) {

        return Collections.emptySet();
    }

    /**
     * Gets a list of source files required for the task. These files will be copied into the root of the isolation
     * session before the task is dispatched to a worker, making them available for download.
     *
     * @param task
     *         The task for which to determine source files.
     *
     * @return A list of paths to the source files on the main server's filesystem.
     */
    default List<Path> getSourceFiles(Task task) {

        return Collections.emptyList();
    }

}
