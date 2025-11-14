package fr.anisekai.server.tasking;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.enums.TaskStatus;

public final class TaskBuilder {

    private final TaskFactory<?> factory;
    private       String         name;
    private       byte           priority = 0;
    private       AnisekaiJson   args     = new AnisekaiJson();

    private TaskBuilder(TaskFactory<?> factory) {

        this.factory = factory;
    }

    public static TaskBuilder of(TaskFactory<?> factory) {

        return new TaskBuilder(factory);
    }

    public TaskBuilder name(String name) {

        this.name = name;
        return this;
    }

    public TaskBuilder priority(byte priority) {

        this.priority = priority;
        return this;
    }

    public TaskBuilder args(AnisekaiJson args) {

        this.args = args;
        return this;
    }

    public TaskFactory<?> getFactory() {

        return this.factory;
    }

    public String getName() {

        return this.name;
    }

    public byte getPriority() {

        return this.priority;
    }

    public AnisekaiJson getArgs() {

        return this.args;
    }

    public Task build() {

        Task task = new Task();
        task.setFactoryName(this.factory.getName());
        task.setName(this.name != null ? this.name : this.factory.getName());
        task.setPriority(this.priority);
        task.setStatus(TaskStatus.SCHEDULED);
        task.setArguments(this.args);

        return task;
    }

}
