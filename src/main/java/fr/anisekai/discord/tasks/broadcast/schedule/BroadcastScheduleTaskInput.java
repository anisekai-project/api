package fr.anisekai.discord.tasks.broadcast.schedule;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BroadcastScheduleTaskInput(
        UUID broadcastId,
        String name,
        String location,
        String description,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {

}
