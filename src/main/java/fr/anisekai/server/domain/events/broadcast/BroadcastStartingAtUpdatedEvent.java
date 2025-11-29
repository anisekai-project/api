package fr.anisekai.server.domain.events.broadcast;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Broadcast;

import java.time.Instant;

public class BroadcastStartingAtUpdatedEvent extends EntityPropertyChangedEvent<Broadcast, Instant> {

    public BroadcastStartingAtUpdatedEvent(Object source, Broadcast entity, Instant oldValue, Instant newValue) {

        super(source, entity, oldValue, newValue);
    }

}
