package fr.anisekai.server.tasking;

import fr.anisekai.server.domain.entities.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.parser.Entity;

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

}
