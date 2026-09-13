package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Selection;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SelectionRepository extends AnisekaiRepository<Selection, UUID> {

}
