package fr.anisekai.server.domain.events.anime;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.enums.AnimeList;

public class AnimeListUpdatedEvent extends EntityPropertyChangedEvent<Anime, AnimeList> {

    public AnimeListUpdatedEvent(Object source, Anime entity, AnimeList previous, AnimeList current) {

        super(source, entity, previous, current);
    }

}
