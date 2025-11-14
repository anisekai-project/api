package fr.anisekai.server.domain.events.anime;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Anime;

public class AnimeSynopsisUpdatedEvent extends EntityPropertyChangedEvent<Anime, String> {

    public AnimeSynopsisUpdatedEvent(Object source, Anime entity, String previous, String current) {

        super(source, entity, previous, current);
    }

}
