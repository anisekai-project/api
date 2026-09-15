package fr.anisekai.library.tasks.torrent.sourcing;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.server.services.TorrentService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class TorrentSourcingTaskFactory implements ServerFactory<Task, TorrentSourcingTaskInput, Nothing>, ClientFactory<TorrentSourcingTaskInput, Nothing> {

    private final JsonSerializerFactory serializerFactory;
    private final AnimeService          animeService;
    private final EpisodeService        episodeService;
    private final TorrentService        torrentService;
    private final TorrentFileService    torrentFileService;


    public TorrentSourcingTaskFactory(JsonSerializerFactory serializerFactory, AnimeService animeService, EpisodeService episodeService, TorrentService torrentService, TorrentFileService torrentFileService) {

        this.serializerFactory  = serializerFactory;
        this.animeService       = animeService;
        this.episodeService     = episodeService;
        this.torrentService     = torrentService;
        this.torrentFileService = torrentFileService;
    }

    @Override
    public @NotNull TaskHandler<TorrentSourcingTaskInput, Nothing> getHandler() {

        return new TorrentSourcingTask(
                this.animeService,
                this.episodeService,
                this.torrentService,
                this.torrentFileService
        );
    }

    @Override
    public @NotNull String getName() {

        return "torrent:sourcing";
    }

    @Override
    public @NotNull ObjectSerializer<TorrentSourcingTaskInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(TorrentSourcingTaskInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getResultSerializer() {

        return this.serializerFactory.emptySerializer();
    }

}
