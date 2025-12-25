package fr.anisekai.server.domain.events.anime;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.Anime;

public class AnimeTotalUpdatedEvent extends EntityUpdatedEvent<Anime, Integer> {

    public AnimeTotalUpdatedEvent(Object source, Anime entity, Integer previous, Integer current) {

        super(source, entity, previous, current);
    }

}
