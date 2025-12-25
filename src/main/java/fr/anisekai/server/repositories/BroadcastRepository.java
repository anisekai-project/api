package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BroadcastRepository extends AnisekaiRepository<Broadcast, Long> {

    List<Broadcast> findAllByStatusIn(Collection<BroadcastStatus> statuses);

    List<Broadcast> findAllByStatus(BroadcastStatus status);

    long countBroadcastByStartingAtAfter(Instant startingAtAfter);

    Optional<Broadcast> findByEventId(Long eventId);

    @Query("select count(b) from Broadcast b where b.watchTarget.id = :id and b.startingAt < :startingAt and b.status IN :statuses")
    long countPreviousOf(Long id, Instant startingAt, Collection<BroadcastStatus> statuses);

    List<Broadcast> findByWatchTargetAndStartingAtAfterOrderByStartingAtAsc(Anime watchTarget, Instant startingAt);


}
