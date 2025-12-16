package fr.anisekai.library.tasks.factories;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.library.Library;
import fr.anisekai.library.tasks.executors.MediaImportTask;
import fr.anisekai.library.tasks.executors.MediaUpdateTask;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskFactory;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class MediaUpdateFactory implements TaskFactory<MediaUpdateTask> {

    public static final String NAME = "media:update";

    private final TaskService    service;
    private final Library        library;
    private final EpisodeService episodeService;

    public MediaUpdateFactory(TaskService service, Library library, TrackService trackService, EpisodeService episodeService) {

        this.service        = service;
        this.library        = library;
        this.episodeService = episodeService;
    }

    @Override
    public @NotNull String getName() {

        return NAME;
    }

    @Override
    public @NotNull MediaUpdateTask create() {

        return new MediaUpdateTask(
                this.library,
                this.episodeService
        );
    }

    @Override
    public boolean hasNamedTask() {

        return true;
    }


    public Task queue(Episode episode) {

        return this.queue(episode, Task.PRIORITY_AUTOMATIC_LOW);
    }

    public Task queue(Episode episode, byte priority) {

        String       name      = String.format("%s:%s", episode.getAnime().getId(), episode.getNumber());
        AnisekaiJson arguments = new AnisekaiJson();

        arguments.put(MediaImportTask.OPTION_EPISODE, episode.getId());

        return this.service.queue(
                TaskBuilder.of(this)
                           .name(name)
                           .args(arguments)
                           .priority(priority)
        );
    }

    @PostConstruct
    private void postConstruct() {

        this.service.registerFactory(TaskPipeline.SOFT, this);
    }

}
