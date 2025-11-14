package fr.anisekai.library.tasks.factories;

import fr.anisekai.library.tasks.executors.TorrentSourcingTask;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.*;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskFactory;
import fr.anisekai.core.internal.json.AnisekaiJson;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class TorrentSourcingFactory implements TaskFactory<TorrentSourcingTask> {

    private static final String NAME = "torrent:sourcing";

    private final TaskService        service;
    private final AnimeService       animeService;
    private final EpisodeService     episodeService;
    private final TorrentService     torrentService;
    private final TorrentFileService torrentFileService;

    public TorrentSourcingFactory(TaskService service, AnimeService animeService, EpisodeService episodeService, TorrentService torrentService, TorrentFileService torrentFileService) {

        this.service            = service;
        this.animeService       = animeService;
        this.episodeService     = episodeService;
        this.torrentService     = torrentService;
        this.torrentFileService = torrentFileService;
    }

    @Override
    public @NotNull String getName() {

        return NAME;
    }

    @Override
    public @NotNull TorrentSourcingTask create() {

        return new TorrentSourcingTask(
                this.animeService,
                this.episodeService,
                this.torrentService,
                this.torrentFileService
        );
    }

    @Override
    public boolean hasNamedTask() {

        return false;
    }

    public Task queue(String source) {

        return this.queue(source, Task.PRIORITY_AUTOMATIC_LOW);
    }

    public Task queue(String source, byte priority) {

        AnisekaiJson arguments = new AnisekaiJson();
        arguments.put(TorrentSourcingTask.OPTION_SOURCE, source);
        return this.service.queue(
                TaskBuilder.of(this)
                           .args(arguments)
                           .priority(priority)
        );
    }

    @PostConstruct
    private void postConstruct() {

        this.service.registerFactory(TaskPipeline.SOFT, this);
    }

}
