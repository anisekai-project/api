package fr.anisekai.server.domain.events.selection;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.enums.SelectionStatus;

public class SelectionStatusUpdatedEvent extends EntityPropertyChangedEvent<Selection, SelectionStatus> {

    public SelectionStatusUpdatedEvent(Object source, Selection entity, SelectionStatus previous, SelectionStatus current) {

        super(source, entity, previous, current);
    }

}
