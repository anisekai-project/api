package fr.anisekai.server.domain.events.broadcast;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.Broadcast;

public class BroadcastFirstEpisodeUpdatedEvent extends EntityUpdatedEvent<Broadcast, Integer> {

    public BroadcastFirstEpisodeUpdatedEvent(Object source, Broadcast entity, Integer oldValue, Integer newValue) {

        super(source, entity, oldValue, newValue);
    }

}
