package fr.anisekai.server.domain.events.voter;

import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;

import java.util.Set;

public class VoterVotesUpdatedEvent extends EntityUpdatedEvent<DiscordUser, Set<Anime>> {

    public VoterVotesUpdatedEvent(Object source, DiscordUser entity, Set<Anime> previous, Set<Anime> current) {

        super(source, entity, previous, current);
    }

}
