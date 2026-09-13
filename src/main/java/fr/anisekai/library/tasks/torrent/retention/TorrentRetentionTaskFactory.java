package fr.anisekai.library.tasks.torrent.retention;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.library.Library;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.server.services.TorrentService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class TorrentRetentionTaskFactory implements ServerFactory<Task, TorrentRetentionInput, Nothing>, ClientFactory<TorrentRetentionInput, Nothing> {

    public static final String NAME = "torrent:cleanup";

    private final JsonSerializerFactory serializerFactory;
    private final TorrentService        torrentService;
    private final TorrentFileService    torrentFileService;
    private final Library               library;

    public TorrentRetentionTaskFactory(JsonSerializerFactory serializerFactory, TorrentService torrentService, TorrentFileService torrentFileService, Library library) {

        this.serializerFactory  = serializerFactory;
        this.torrentService     = torrentService;
        this.torrentFileService = torrentFileService;
        this.library            = library;
    }

    @Override
    public @NotNull String getName() {

        return NAME;
    }

    @Override
    public @NotNull TaskHandler<TorrentRetentionInput, Nothing> getHandler() {

        return new TorrentRetentionTask(this.library, this.torrentService, this.torrentFileService);
    }

    @Override
    public @NotNull ObjectSerializer<TorrentRetentionInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(TorrentRetentionInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getResultSerializer() {

        return this.serializerFactory.emptySerializer();
    }

}
