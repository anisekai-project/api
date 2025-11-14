package fr.anisekai.discord.tasks.watchlist.update;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.WatchlistService;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskExecutor;
import fr.anisekai.server.tasking.TaskFactory;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class WatchlistUpdateFactory implements TaskFactory<WatchlistUpdateTask> {

    public static final String NAME = "watchlist:update";

    private final TaskService      service;
    private final AnimeService     animeService;
    private final InterestService  interestService;
    private final WatchlistService watchlistService;
    private final JDAStore         store;

    public WatchlistUpdateFactory(TaskService service, AnimeService animeService, InterestService interestService, WatchlistService watchlistService, JDAStore store) {

        this.service          = service;
        this.animeService     = animeService;
        this.interestService  = interestService;
        this.watchlistService = watchlistService;
        this.store            = store;
    }

    /**
     * Get this {@link TaskFactory} name, which will be used to associate it with a {@link Task}.
     *
     * @return The {@link TaskFactory} name.
     */
    @Override
    public @NotNull String getName() {

        return NAME;
    }

    /**
     * Create an instance of {@link TaskExecutor}. This is useful if your task has some bean dependencies.
     *
     * @return A new {@link TaskExecutor} instance.
     */
    @Override
    public @NotNull WatchlistUpdateTask create() {

        return new WatchlistUpdateTask(
                this.animeService,
                this.interestService,
                this.watchlistService,
                this.store
        );
    }

    @Override
    public boolean hasNamedTask() {

        return true;
    }

    public Task queue(AnimeList list) {

        return this.queue(list, Task.PRIORITY_AUTOMATIC_LOW);
    }

    public Task queue(AnimeList list, byte priority) {

        String       name      = String.format("%s:%s", this.getName(), list.name().toLowerCase());
        AnisekaiJson arguments = new AnisekaiJson();
        arguments.put(WatchlistUpdateTask.OPTION_WATCHLIST, list.name().toLowerCase());

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
