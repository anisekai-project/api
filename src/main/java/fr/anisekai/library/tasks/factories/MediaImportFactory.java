package fr.anisekai.library.tasks.factories;

import fr.anisekai.library.Library;
import fr.anisekai.library.tasks.executors.MediaImportTask;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskFactory;
import fr.anisekai.core.internal.json.AnisekaiJson;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class MediaImportFactory implements TaskFactory<MediaImportTask> {

    public static final String NAME = "media:import";

    private final TaskService    service;
    private final Library        library;
    private final TrackService   trackService;
    private final EpisodeService episodeService;

    public MediaImportFactory(TaskService service, Library library, TrackService trackService, EpisodeService episodeService) {

        this.service        = service;
        this.library        = library;
        this.trackService   = trackService;
        this.episodeService = episodeService;
    }

    @Override
    public @NotNull String getName() {

        return NAME;
    }

    @Override
    public @NotNull MediaImportTask create() {

        return new MediaImportTask(
                this.library,
                this.trackService,
                this.episodeService
        );
    }

    @Override
    public boolean hasNamedTask() {

        return true;
    }

    public List<Task> queue(Torrent torrent) {

        List<Task> tasks = new ArrayList<>();
        for (TorrentFile file : torrent.getFiles()) {
            Path source = this.library
                    .findDownload(file)
                    .orElseThrow(() -> new IllegalStateException(
                            String.format(
                                    "Could not determine path to file %s of torrent %s",
                                    file.getIndex(),
                                    file.getTorrent().getId()
                            )));

            Episode episode = file.getEpisode();
            tasks.add(this.queue(source, episode, torrent.getPriority()));
        }

        return tasks;
    }

    public Task queue(Path source, Episode episode) {

        return this.queue(source, episode, Task.PRIORITY_AUTOMATIC_LOW);
    }

    public Task queue(Path source, Episode episode, byte priority) {

        String       name      = String.format("%s:%s", episode.getAnime().getId(), episode.getNumber());
        AnisekaiJson arguments = new AnisekaiJson();

        arguments.put(MediaImportTask.OPTION_SOURCE, source.toString());
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

        this.service.registerFactory(TaskPipeline.HEAVY, this);
    }

}
