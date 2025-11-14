package fr.anisekai.server.domain.events.user;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.DiscordUser;

public class UserActiveUpdatedEvent extends EntityPropertyChangedEvent<DiscordUser, Boolean> {

    public UserActiveUpdatedEvent(Object source, DiscordUser entity, Boolean previous, Boolean current) {

        super(source, entity, previous, current);
    }

}
