package fr.anisekai.server.repositories;

import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Torrent;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TorrentRepository extends AnisekaiRepository<Torrent, UUID> {

    List<Torrent> findByStatusIn(Collection<Transmission.TorrentStatus> statuses);

    List<Torrent> findByStatusInAndUpdatedAtLessThan(Collection<Transmission.TorrentStatus> statuses, ZonedDateTime updatedAt);

}
