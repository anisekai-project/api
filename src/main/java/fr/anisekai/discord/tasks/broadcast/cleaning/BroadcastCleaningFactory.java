package fr.anisekai.discord.tasks.broadcast.cleaning;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskFactory;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;

public class BroadcastCleaningFactory implements TaskFactory<BroadcastCleaningTask> {

    public static final String           NAME = "broadcast:cleaning";
    private final       TaskService      service;
    private final       BroadcastService broadcastService;
    private final       JDAStore         store;

    public BroadcastCleaningFactory(TaskService service, BroadcastService broadcastService, JDAStore store) {

        this.service          = service;
        this.broadcastService = broadcastService;
        this.store            = store;
    }

    @Override
    public @NotNull String getName() {

        return NAME;
    }

    @Override
    public @NotNull BroadcastCleaningTask create() {

        return new BroadcastCleaningTask(this.broadcastService, this.store);
    }

    @Override
    public boolean hasNamedTask() {

        return false;
    }

    public Task queue() {

        return this.queue(Task.PRIORITY_AUTOMATIC_LOW);
    }

    public Task queue(byte priority) {

        return this.service.queue(
                TaskBuilder.of(this)
                           .priority(priority)
        );
    }

    @PostConstruct
    private void postConstruct() {

        this.service.registerFactory(TaskPipeline.SOFT, this);
    }

}
