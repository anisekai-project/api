package fr.anisekai.server.domain.events.anime;

import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Anime;

import java.util.List;

public class AnimeTagsUpdatedEvent extends EntityPropertyChangedEvent<Anime, List<String>> {

    public AnimeTagsUpdatedEvent(Object source, Anime entity, List<String> previous, List<String> current) {

        super(source, entity, previous, current);
    }

}
