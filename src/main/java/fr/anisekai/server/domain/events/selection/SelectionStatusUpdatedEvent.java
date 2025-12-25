package fr.anisekai.server.domain.events.selection;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.enums.SelectionStatus;

public class SelectionStatusUpdatedEvent extends EntityUpdatedEvent<Selection, SelectionStatus> {

    public SelectionStatusUpdatedEvent(Object source, Selection entity, SelectionStatus previous, SelectionStatus current) {

        super(source, entity, previous, current);
    }

}
