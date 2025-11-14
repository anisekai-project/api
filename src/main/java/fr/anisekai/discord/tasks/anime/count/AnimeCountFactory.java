package fr.anisekai.discord.tasks.anime.count;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskFactory;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class AnimeCountFactory implements TaskFactory<AnimeCountTask> {

    public static final String       NAME = "anime:count";
    private final       TaskService  service;
    private final       AnimeService animeService;
    private final       JDAStore     store;

    public AnimeCountFactory(TaskService service, AnimeService animeService, JDAStore store) {

        this.service      = service;
        this.animeService = animeService;
        this.store        = store;
    }

    @Override
    public @NotNull String getName() {

        return NAME;
    }

    @Override
    public @NotNull AnimeCountTask create() {

        return new AnimeCountTask(this.animeService, this.store);
    }

    @Override
    public boolean hasNamedTask() {

        return false;
    }

    public Task queue() {

        return this.queue(Task.PRIORITY_DEFAULT);
    }

    public Task queue(byte priority) {

        return this.service.queue(
                TaskBuilder.of(this)
                           .priority(priority)
        );
    }

    @PostConstruct
    private void postConstruct() {

        this.service.registerFactory(TaskPipeline.MESSAGING, this);
    }

}
