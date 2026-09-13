package fr.anisekai.discord.tasks.broadcast.schedule;

import net.dv8tion.jda.api.entities.ScheduledEvent;

public record BroadcastScheduleTaskOutput(
        long eventId,
        ScheduledEvent.Status status
) {

}
