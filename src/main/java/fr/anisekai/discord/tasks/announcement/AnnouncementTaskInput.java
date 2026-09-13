package fr.anisekai.discord.tasks.announcement;

import java.util.UUID;

public record AnnouncementTaskInput(
        UUID animeId,
        boolean edit
) {

}
