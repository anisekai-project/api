package fr.anisekai.server.domain.events.broadcast;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Broadcast;

import java.time.ZonedDateTime;

public class BroadcastStartingAtUpdatedEvent extends EntityPropertyChangedEvent<Broadcast, ZonedDateTime> {

    public BroadcastStartingAtUpdatedEvent(Object source, Broadcast entity, ZonedDateTime oldValue, ZonedDateTime newValue) {

        super(source, entity, oldValue, newValue);
    }

}
