package fr.anisekai.discord.tasks.broadcast.cancel;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.exceptions.task.FatalTaskException;
import fr.anisekai.server.services.BroadcastService;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import org.jspecify.annotations.NonNull;

public class BroadcastCancelTask implements TaskHandler<BroadcastCancelTaskInput, Nothing> {

    private final BroadcastService service;
    private final JDAStore         store;

    public BroadcastCancelTask(BroadcastService service, JDAStore store) {

        this.service = service;
        this.store   = store;
    }

    @Override
    public @NonNull Nothing handle(@NonNull BroadcastCancelTaskInput arguments) {

        Broadcast broadcast = this.service.requireById(arguments.broadcastId());

        if (broadcast.getEventId() == null) {
            throw new FatalTaskException("Can't cancel broadcast with no event ID.");
        }

        ScheduledEvent event = this.store.requireGuild().getScheduledEventById(broadcast.getEventId());

        if (event == null) {
            throw new IllegalStateException("Could not find the associated scheduled event.");
        }

        if (event.getStatus() == ScheduledEvent.Status.ACTIVE) {
            event.getManager().setStatus(ScheduledEvent.Status.COMPLETED).complete();
        } else if (event.getStatus() == ScheduledEvent.Status.SCHEDULED) {
            event.getManager().setStatus(ScheduledEvent.Status.CANCELED).complete();
        } else {
            throw new FatalTaskException("Could not update scheduled event with list " + event.getStatus());
        }

        return Nothing.INSTANCE;
    }

}
