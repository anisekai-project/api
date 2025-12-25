package fr.anisekai.server.domain.events.user;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.DiscordUser;

public class UserActiveUpdatedEvent extends EntityUpdatedEvent<DiscordUser, Boolean> {

    public UserActiveUpdatedEvent(Object source, DiscordUser entity, Boolean previous, Boolean current) {

        super(source, entity, previous, current);
    }

}
