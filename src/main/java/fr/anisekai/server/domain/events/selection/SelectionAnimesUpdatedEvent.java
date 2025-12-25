package fr.anisekai.server.domain.events.selection;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Selection;

import java.util.Set;

public class SelectionAnimesUpdatedEvent extends EntityUpdatedEvent<Selection, Set<Anime>> {

    public SelectionAnimesUpdatedEvent(Object source, Selection entity, Set<Anime> previous, Set<Anime> current) {

        super(source, entity, previous, current);
    }

}
