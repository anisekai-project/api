package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrackRepository extends AnisekaiRepository<Track, UUID> {

    List<Track> findByEpisode(Episode episode);

    long deleteByEpisode(Episode episode);

}
