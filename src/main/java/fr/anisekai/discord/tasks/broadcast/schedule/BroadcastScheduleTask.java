package fr.anisekai.discord.tasks.broadcast.schedule;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.tasks.broadcast.BroadcastTask;
import fr.anisekai.library.Library;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.services.BroadcastService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastScheduleTask extends BroadcastTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastScheduleTask.class);

    public BroadcastScheduleTask(Library library, JDAStore store, BroadcastService service) {

        super(library, store, service);
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) throws Exception {

        Broadcast broadcast = this.getService().requireById(params.getLong(OPT_BROADCAST));
        EventData data      = this.getEventData(broadcast);
        Guild     guild     = this.getGuild();

        if (broadcast.getStatus() == BroadcastStatus.ACTIVE) {
            throw new IllegalStateException("Can't update or schedule an active broadcast.");
        }

        if (broadcast.getStatus() == BroadcastStatus.COMPLETED) {
            throw new IllegalStateException("Can't update or schedule an completed broadcast.");
        }

        if (broadcast.getStatus() == BroadcastStatus.CANCELED) {
            throw new IllegalStateException("Can't update or schedule an canceled broadcast.");
        }

        if (broadcast.getEventId() != null) {
            // Just refresh the existing event.
            ScheduledEvent event = guild.getScheduledEventById(broadcast.getEventId());

            if (event == null) {
                throw new IllegalStateException("Could not find the associated scheduled event.");
            }

            event.getManager()
                 .setName(data.title)
                 .setLocation("Anisekai")
                 .setDescription(data.description)
                 .setStartTime(data.startTime)
                 .setEndTime(data.endTime)
                 .setImage(data.icon)
                 .complete();

            return;
        }

        ScheduledEvent event = guild
                .createScheduledEvent(data.title, "Anisekai", data.startTime, data.endTime)
                .setDescription(data.description)
                .setImage(data.icon)
                .complete();

        this.getService().mod(
                broadcast.getId(),
                entity -> {
                    entity.setEventId(event.getIdLong());
                    entity.setStatus(BroadcastStatus.SCHEDULED);
                }
        );
    }

}
