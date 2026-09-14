package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Worker;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerRepository extends AnisekaiRepository<Worker, UUID> {

    Optional<Worker> findByIdAndSessionTokenId(UUID workerId, UUID sessionTokenId);
}