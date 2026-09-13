package fr.anisekai.discord.tasks.watchlist.update;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.WatchlistService;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class WatchlistUpdateTaskFactory implements ServerFactory<Task, WatchlistUpdateTaskInput, Nothing>, ClientFactory<WatchlistUpdateTaskInput, Nothing> {

    private final JsonSerializerFactory serializerFactory;
    private final WatchlistService      watchlistService;
    private final AnimeService          animeService;
    private final InterestService       interestService;
    private final JDAStore              store;

    public WatchlistUpdateTaskFactory(JsonSerializerFactory serializerFactory, WatchlistService watchlistService, AnimeService animeService, InterestService interestService, JDAStore store) {

        this.serializerFactory = serializerFactory;
        this.watchlistService  = watchlistService;
        this.animeService      = animeService;
        this.interestService   = interestService;
        this.store             = store;
    }

    @Override
    public @NotNull String getName() {

        return "watchlist:update";
    }

    @Override
    public @NotNull String getTaskName(@NonNull WatchlistUpdateTaskInput arguments) {

        return String.format("%s:%s", this.getName(), arguments.list().name().toLowerCase(Locale.ROOT));
    }

    @Override
    public @NotNull TaskHandler<WatchlistUpdateTaskInput, Nothing> getHandler() {

        return new WatchlistUpdateTask(this.watchlistService, this.animeService, this.interestService, this.store);
    }

    @Override
    public @NotNull ObjectSerializer<WatchlistUpdateTaskInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(WatchlistUpdateTaskInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getResultSerializer() {

        return this.serializerFactory.emptySerializer();
    }

}
