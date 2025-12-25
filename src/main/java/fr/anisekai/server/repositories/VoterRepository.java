package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import fr.anisekai.server.domain.keys.VoterKey;

import java.util.List;
import java.util.Optional;

public interface VoterRepository extends AnisekaiRepository<Voter, VoterKey> {

    Optional<Voter> findBySelectionAndUser(Selection selection, DiscordUser user);

    List<Voter> findBySelection(Selection selection);

}
