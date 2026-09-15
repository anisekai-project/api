package fr.anisekai.discord.tasks.broadcast.schedule;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.library.Library;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.utils.DiscordUtils;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class BroadcastScheduleTaskFactory implements ServerFactory<Task, BroadcastScheduleTaskInput, BroadcastScheduleTaskOutput>, ClientFactory<BroadcastScheduleTaskInput, BroadcastScheduleTaskOutput> {

    private final JsonSerializerFactory serializerFactory;
    private final Library               library;
    private final JDAStore              store;
    private final BroadcastService      broadcastService;

    public BroadcastScheduleTaskFactory(JsonSerializerFactory serializerFactory, Library library, JDAStore store, BroadcastService broadcastService) {

        this.serializerFactory = serializerFactory;
        this.library           = library;
        this.store             = store;
        this.broadcastService  = broadcastService;
    }

    public static BroadcastScheduleTaskInput createInput(Broadcast broadcast) {

        return new BroadcastScheduleTaskInput(
                broadcast.getId(),
                "Soirée Anime",
                "Anisekai",
                String.format(
                        "**%s**\nÉpisode(s): %s",
                        broadcast.getWatchTarget().getTitle(),
                        DiscordUtils.getEpisodeText(broadcast)
                ),
                broadcast.getStartingAt().atOffset(ZoneOffset.UTC),
                broadcast.getEndingAt().atOffset(ZoneOffset.UTC)
        );
    }

    @Override
    public @NotNull String getName() {

        return "broadcast:schedule";
    }

    @Override
    public @NotNull String getTaskName(@NonNull BroadcastScheduleTaskInput arguments) {

        return String.format("%s:%s", this.getName(), arguments.broadcastId());
    }

    @Override
    public @NotNull TaskHandler<BroadcastScheduleTaskInput, BroadcastScheduleTaskOutput> getHandler() {

        return new BroadcastScheduleTask(this.library, this.store, this.broadcastService);
    }

    @Override
    public @NotNull ObjectSerializer<BroadcastScheduleTaskInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(BroadcastScheduleTaskInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<BroadcastScheduleTaskOutput> getResultSerializer() {

        return this.serializerFactory.createSerializer(BroadcastScheduleTaskOutput.class);
    }

    @Override
    public void onSuccess(@NotNull TaskExecutedPacket<Task, BroadcastScheduleTaskOutput> packet) {

        BroadcastScheduleTaskInput arguments = this.getArgumentsSerializer().deserialize(packet.task().getArguments());

        this.broadcastService.mod(
                arguments.broadcastId(), broadcast -> {
                    broadcast.setEventId(packet.result().eventId());
                    broadcast.setStatus(BroadcastStatus.SCHEDULED);
                }
        );
    }

}
