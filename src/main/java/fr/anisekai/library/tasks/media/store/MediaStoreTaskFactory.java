package fr.anisekai.library.tasks.media.store;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.tasking.IsolatedServerFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MediaStoreTaskFactory implements ServerFactory<Task, MediaStoreTaskInput, Nothing>, IsolatedServerFactory<MediaStoreTaskInput> {

    private final JsonSerializerFactory serializerFactory;
    private final EpisodeService        episodeService;

    public MediaStoreTaskFactory(JsonSerializerFactory serializerFactory, EpisodeService episodeService) {

        this.serializerFactory = serializerFactory;
        this.episodeService    = episodeService;
    }

    @Override
    public @NotNull String getName() {

        return "media:store";
    }

    @Override
    public @NotNull String getTaskName(@NotNull MediaStoreTaskInput arguments) {

        return "%s:%s".formatted(this.getName(), arguments.episode());
    }

    @Override
    public @NotNull ObjectSerializer<MediaStoreTaskInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(MediaStoreTaskInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getResultSerializer() {

        return this.serializerFactory.emptySerializer();
    }

    @Override
    public @NotNull Set<AccessScope> getIsolationScopes(@NotNull Task task, @NotNull MediaStoreTaskInput input) {

        Episode episode = this.episodeService.requireById(input.episode());
        AccessScope episodeScope = new AccessScope(Library.EPISODES, episode.getScopedName());
        AccessScope chunksScope = new AccessScope(Library.CHUNKS, episode.getScopedName());
        AccessScope subtitleScope = new AccessScope(Library.SUBTITLES, episode.getScopedName());

        if (input.refresh()) {
            return Set.of(episodeScope, chunksScope, subtitleScope);
        }
        return Set.of(chunksScope, subtitleScope);
    }

    @Override
    public void onSuccess(@NotNull TaskExecutedPacket<Task, Nothing> packet) {

        MediaStoreTaskInput input = this.getArgumentsSerializer().deserialize(packet.task().getArguments());
        this.episodeService.mod(input.episode(), episode -> episode.setReady(true));
    }

}
