package fr.anisekai.library.tasks.media.store;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.EpisodeService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class MediaStoreTaskFactory implements ServerFactory<Task, MediaStoreTaskInput, Nothing> {

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
    public void onSuccess(@NotNull TaskExecutedPacket<Task, Nothing> packet) {

        MediaStoreTaskInput input = this.getArgumentsSerializer().deserialize(packet.task().getArguments());
        this.episodeService.mod(input.episode(), episode -> episode.setReady(true));
    }

}
