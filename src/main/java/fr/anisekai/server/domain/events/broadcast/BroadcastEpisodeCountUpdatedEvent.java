package fr.anisekai.server.domain.events.broadcast;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Broadcast;

public class BroadcastEpisodeCountUpdatedEvent extends EntityPropertyChangedEvent<Broadcast, Integer> {

    public BroadcastEpisodeCountUpdatedEvent(Object source, Broadcast entity, Integer oldValue, Integer newValue) {

        super(source, entity, oldValue, newValue);
    }

}
