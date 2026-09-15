package fr.anisekai.library.tasks.media.store;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.library.Library;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.services.EpisodeService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class MediaStoreClientFactory implements ClientFactory<MediaStoreTaskInput, Nothing> {

    private final JsonSerializerFactory serializerFactory;
    private final Library               library;
    private final EpisodeService        episodeService;

    public MediaStoreClientFactory(JsonSerializerFactory serializerFactory, Library library, EpisodeService episodeService) {

        this.serializerFactory = serializerFactory;
        this.library           = library;
        this.episodeService    = episodeService;
    }

    @Override
    public @NotNull String getName() {

        return "media:store";
    }

    @Override
    public @NotNull TaskHandler<MediaStoreTaskInput, Nothing> getHandler() {

        return new MediaStoreTask(this.library, this.episodeService);
    }

    @Override
    public @NotNull ObjectSerializer<MediaStoreTaskInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(MediaStoreTaskInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getResultSerializer() {

        return this.serializerFactory.emptySerializer();
    }

}
