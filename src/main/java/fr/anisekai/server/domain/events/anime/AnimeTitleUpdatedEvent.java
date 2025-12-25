package fr.anisekai.server.domain.events.anime;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.Anime;

public class AnimeTitleUpdatedEvent extends EntityUpdatedEvent<Anime, String> {

    public AnimeTitleUpdatedEvent(Object source, Anime entity, String previous, String current) {

        super(source, entity, previous, current);
    }

}
