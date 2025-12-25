package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Episode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeRepository extends AnisekaiRepository<Episode, Long> {

    Optional<Episode> findByAnimeAndNumber(Anime anime, int number);

    List<Episode> findAllByReadyTrue();

}
