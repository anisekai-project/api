package fr.anisekai.discord.tasks.watchlist.create;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.WatchlistService;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskFactory;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class WatchlistCreateFactory implements TaskFactory<WatchlistCreateTask> {

    public static final String NAME = "watchlist:create";

    private final TaskService      service;
    private final JDAStore         store;
    private final AnimeService     animeService;
    private final InterestService  interestService;
    private final WatchlistService watchlistService;

    public WatchlistCreateFactory(TaskService service, JDAStore store, AnimeService animeService, InterestService interestService, WatchlistService watchlistService) {

        this.service          = service;
        this.store            = store;
        this.animeService     = animeService;
        this.interestService  = interestService;
        this.watchlistService = watchlistService;
    }

    @Override
    public @NotNull String getName() {

        return NAME;
    }

    @Override
    public @NotNull WatchlistCreateTask create() {

        return new WatchlistCreateTask(this.store, this.watchlistService, this.animeService, this.interestService);
    }

    @Override
    public boolean hasNamedTask() {

        return false;
    }

    public Task queue() {

        return this.queue(Task.PRIORITY_AUTOMATIC_HIGH);
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
