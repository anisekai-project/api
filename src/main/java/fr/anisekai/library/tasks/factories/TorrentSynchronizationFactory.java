package fr.anisekai.library.tasks.factories;

import fr.anisekai.library.tasks.executors.TorrentSynchronizationTask;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.TorrentService;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskFactory;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class TorrentSynchronizationFactory implements TaskFactory<TorrentSynchronizationTask> {

    private final static String         NAME = "torrent:sync";
    private final        TaskService    service;
    private final        TorrentService torrentService;

    public TorrentSynchronizationFactory(TaskService service, TorrentService torrentService) {

        this.service        = service;
        this.torrentService = torrentService;
    }

    @Override
    public @NotNull String getName() {

        return NAME;
    }

    @Override
    public @NotNull TorrentSynchronizationTask create() {

        return new TorrentSynchronizationTask(this.service, this.torrentService);
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
