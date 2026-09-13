package fr.anisekai.discord.tasks.announcement;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementTaskFactory implements ServerFactory<Task, AnnouncementTaskInput, AnnouncementTaskOutput>, ClientFactory<AnnouncementTaskInput, AnnouncementTaskOutput> {

    private final JsonSerializerFactory serializerFactory;
    private final JDAStore              store;
    private final AnimeService          animeService;
    private final InterestService       interestService;

    public AnnouncementTaskFactory(JsonSerializerFactory serializerFactory, JDAStore store, AnimeService animeService, InterestService interestService) {

        this.serializerFactory = serializerFactory;
        this.store             = store;
        this.animeService      = animeService;
        this.interestService   = interestService;
    }

    @Override
    public @NotNull String getName() {

        return "discord:announcement";
    }

    @Override
    public @NotNull String getTaskName(@NotNull AnnouncementTaskInput arguments) {

        return String.format("%s:%s", this.getName(), arguments.animeId());
    }

    @Override
    public @NotNull TaskHandler<AnnouncementTaskInput, AnnouncementTaskOutput> getHandler() {

        return new AnnouncementTask(this.store, this.animeService, this.interestService);
    }

    @Override
    public @NotNull ObjectSerializer<AnnouncementTaskInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(AnnouncementTaskInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<AnnouncementTaskOutput> getResultSerializer() {

        return this.serializerFactory.createSerializer(AnnouncementTaskOutput.class);
    }

    @Override
    public void onSuccess(@NotNull TaskExecutedPacket<Task, AnnouncementTaskOutput> packet) {

        AnnouncementTaskInput input = this.getArgumentsSerializer().deserialize(packet.task().getArguments());
        this.animeService.mod(input.animeId(), anime -> anime.setAnnouncementId(packet.result().messageId()));
    }

}
