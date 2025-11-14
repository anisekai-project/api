package fr.anisekai.server.domain.events.anime;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Anime;

public class AnimeWatchedUpdatedEvent extends EntityPropertyChangedEvent<Anime, Integer> {

    public AnimeWatchedUpdatedEvent(Object source, Anime entity, Integer previous, Integer current) {

        super(source, entity, previous, current);
    }

}
