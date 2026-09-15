package fr.anisekai.library.tasks.torrent.synchronization;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.TorrentService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class TorrentSynchronizationTaskFactory implements ServerFactory<Task, Nothing, Nothing>, ClientFactory<Nothing, Nothing> {

    private final JsonSerializerFactory serializerFactory;
    private final TorrentService        torrentService;

    public TorrentSynchronizationTaskFactory(JsonSerializerFactory serializerFactory, TorrentService torrentService) {

        this.serializerFactory = serializerFactory;
        this.torrentService    = torrentService;
    }

    @Override
    public @NotNull TaskHandler<Nothing, Nothing> getHandler() {

        return new TorrentSynchronizationTask(this.torrentService);
    }

    @Override
    public @NotNull String getName() {

        return "torrent:synchronize";
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getArgumentsSerializer() {

        return this.serializerFactory.emptySerializer();
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getResultSerializer() {

        return this.serializerFactory.emptySerializer();
    }

}
