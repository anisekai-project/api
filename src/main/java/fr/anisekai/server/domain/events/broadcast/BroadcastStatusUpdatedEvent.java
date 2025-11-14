package fr.anisekai.server.domain.events.broadcast;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.enums.BroadcastStatus;

public class BroadcastStatusUpdatedEvent extends EntityPropertyChangedEvent<Broadcast, BroadcastStatus> {

    public BroadcastStatusUpdatedEvent(Object source, Broadcast entity, BroadcastStatus previous, BroadcastStatus current) {

        super(source, entity, previous, current);
    }

}
