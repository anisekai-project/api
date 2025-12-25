package fr.anisekai.server.domain.events.user;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.DiscordUser;

public class UserEmoteUpdatedEvent extends EntityUpdatedEvent<DiscordUser, String> {

    public UserEmoteUpdatedEvent(Object source, DiscordUser entity, String previous, String current) {

        super(source, entity, previous, current);
    }

}
