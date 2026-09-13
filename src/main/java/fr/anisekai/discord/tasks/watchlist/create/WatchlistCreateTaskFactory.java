package fr.anisekai.discord.tasks.watchlist.create;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.WatchlistService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WatchlistCreateTaskFactory implements ServerFactory<Task, Nothing, WatchlistCreateTaskOutput>, ClientFactory<Nothing, WatchlistCreateTaskOutput> {

    private final JsonSerializerFactory serializerFactory;
    private final JDAStore              store;
    private final WatchlistService      watchlistService;
    private final AnimeService          animeService;
    private final InterestService       interestService;

    public WatchlistCreateTaskFactory(JsonSerializerFactory serializerFactory, JDAStore store, AnimeService animeService, InterestService interestService, WatchlistService watchlistService) {

        this.serializerFactory = serializerFactory;
        this.store             = store;
        this.watchlistService  = watchlistService;
        this.animeService      = animeService;
        this.interestService   = interestService;
    }

    @Override
    public @NotNull String getName() {

        return "watchlist:create";
    }

    @Override
    public @NotNull TaskHandler<Nothing, WatchlistCreateTaskOutput> getHandler() {

        return new WatchlistCreateTask(this.store, this.watchlistService, this.animeService, this.interestService);
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getArgumentsSerializer() {

        return this.serializerFactory.emptySerializer();
    }

    @Override
    public @NotNull ObjectSerializer<WatchlistCreateTaskOutput> getResultSerializer() {

        return this.serializerFactory.createSerializer(WatchlistCreateTaskOutput.class);
    }

    @Override
    public void onSuccess(@NotNull TaskExecutedPacket<Task, WatchlistCreateTaskOutput> packet) {

        for (Map.Entry<AnimeList, Long> entry : packet.result().listMessageMap().entrySet()) {
            this.watchlistService.mod(entry.getKey(), watchlist -> watchlist.setMessageId(entry.getValue()));
        }
    }

}
