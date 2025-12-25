package fr.anisekai.server.domain.events.user;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.DiscordUser;

public class UserUsernameUpdatedEvent extends EntityUpdatedEvent<DiscordUser, String> {

    public UserUsernameUpdatedEvent(Object source, DiscordUser entity, String previous, String current) {

        super(source, entity, previous, current);
    }

}
