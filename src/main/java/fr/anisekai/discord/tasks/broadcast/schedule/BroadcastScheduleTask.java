package fr.anisekai.discord.tasks.broadcast.schedule;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.library.Library;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.exceptions.task.FatalTaskException;
import fr.anisekai.server.services.BroadcastService;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BroadcastScheduleTask implements TaskHandler<BroadcastScheduleTaskInput, BroadcastScheduleTaskOutput> {

    private final Library          library;
    private final JDAStore         store;
    private final BroadcastService service;

    public BroadcastScheduleTask(Library library, JDAStore store, BroadcastService service) {

        this.library = library;
        this.store   = store;
        this.service = service;
    }

    private @Nullable Icon getEventImage(ScopedEntity target) throws IOException {

        Path path = this.library.getResolver(Library.EVENT_IMAGES).file(target);

        if (Files.exists(path)) {
            return Icon.from(path.toFile(), Icon.IconType.PNG);
        }
        return null;
    }

    @Override
    public @NonNull BroadcastScheduleTaskOutput handle(@NonNull BroadcastScheduleTaskInput arguments) throws Exception {

        Broadcast broadcast = this.service.requireById(arguments.broadcastId());

        if (broadcast.getStatus() == BroadcastStatus.ACTIVE) {
            throw new FatalTaskException("Can't update or schedule an active broadcast.");
        }

        if (broadcast.getStatus() == BroadcastStatus.COMPLETED) {
            throw new FatalTaskException("Can't update or schedule an completed broadcast.");
        }

        if (broadcast.getStatus() == BroadcastStatus.CANCELED) {
            throw new FatalTaskException("Can't update or schedule an canceled broadcast.");
        }

        if (broadcast.getEventId() != null) {
            return this.update(arguments, broadcast.getWatchTarget(), broadcast.getEventId());
        }

        return this.schedule(arguments, broadcast.getWatchTarget());
    }

    private BroadcastScheduleTaskOutput update(BroadcastScheduleTaskInput arguments, ScopedEntity anime, long eventId) throws IOException {

        ScheduledEvent event = this.store.requireGuild().getScheduledEventById(eventId);

        if (event == null) {
            throw new FatalTaskException("Could not find the associated scheduled event.");
        }

        Icon image = this.getEventImage(anime);

        event.getManager()
             .setName(arguments.name())
             .setLocation(arguments.location())
             .setDescription(arguments.description())
             .setStartTime(arguments.startTime())
             .setEndTime(arguments.endTime())
             .setImage(image)
             .complete();

        return new BroadcastScheduleTaskOutput(event.getIdLong(), event.getStatus());
    }

    private BroadcastScheduleTaskOutput schedule(BroadcastScheduleTaskInput arguments, ScopedEntity anime) throws IOException {

        Icon image = this.getEventImage(anime);

        ScheduledEvent event = this
                .store
                .requireGuild()
                .createScheduledEvent(
                        arguments.name(),
                        arguments.location(),
                        arguments.startTime(),
                        arguments.endTime()
                )
                .setDescription(arguments.description())
                .setImage(image)
                .complete();

        return new BroadcastScheduleTaskOutput(event.getIdLong(), event.getStatus());
    }

}
