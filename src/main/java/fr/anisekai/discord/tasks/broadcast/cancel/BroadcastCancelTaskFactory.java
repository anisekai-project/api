package fr.anisekai.discord.tasks.broadcast.cancel;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.services.BroadcastService;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class BroadcastCancelTaskFactory implements ServerFactory<Task, BroadcastCancelTaskInput, Nothing>, ClientFactory<BroadcastCancelTaskInput, Nothing> {

    private final JsonSerializerFactory serializerFactory;
    private final JDAStore              store;
    private final BroadcastService      broadcastService;

    public BroadcastCancelTaskFactory(JsonSerializerFactory serializerFactory, JDAStore store, BroadcastService broadcastService) {

        this.serializerFactory = serializerFactory;
        this.store             = store;
        this.broadcastService  = broadcastService;
    }

    @Override
    public @NotNull String getName() {

        return "broadcast:cancel";
    }

    @Override
    public @NotNull String getTaskName(@NonNull BroadcastCancelTaskInput arguments) {

        return String.format("%s:%s", this.getName(), arguments.broadcastId());
    }

    @Override
    public @NotNull TaskHandler<BroadcastCancelTaskInput, Nothing> getHandler() {

        return new BroadcastCancelTask(this.broadcastService, this.store);
    }

    @Override
    public @NotNull ObjectSerializer<BroadcastCancelTaskInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(BroadcastCancelTaskInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<Nothing> getResultSerializer() {

        return this.serializerFactory.emptySerializer();
    }

    @Override
    public void onSuccess(@NotNull TaskExecutedPacket<Task, Nothing> packet) {

        BroadcastCancelTaskInput arguments = this.getArgumentsSerializer().deserialize(packet.task().getArguments());
        this.broadcastService.mod(arguments.broadcastId(), broadcast -> broadcast.setStatus(BroadcastStatus.CANCELED));
    }

}
