package fr.anisekai.server.domain.events.interest;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.Interest;

public class InterestLevelUpdatedEvent extends EntityUpdatedEvent<Interest, Byte> {

    public InterestLevelUpdatedEvent(Object source, Interest entity, Byte previous, Byte current) {

        super(source, entity, previous, current);
    }

}
