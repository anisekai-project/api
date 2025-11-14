package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.keys.InterestKey;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InterestRepository extends AnisekaiRepository<Interest, InterestKey> {

    List<Interest> findByAnime(Anime anime);

    List<Interest> findByUser(DiscordUser user);

    List<Interest> findByAnimeIn(Collection<Anime> anime);

}
