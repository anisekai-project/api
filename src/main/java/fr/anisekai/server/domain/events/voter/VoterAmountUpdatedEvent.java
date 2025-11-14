package fr.anisekai.server.domain.events.voter;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.DiscordUser;

public class VoterAmountUpdatedEvent extends EntityPropertyChangedEvent<DiscordUser, Short> {

    public VoterAmountUpdatedEvent(Object source, DiscordUser entity, Short previous, Short current) {

        super(source, entity, previous, current);
    }

}
