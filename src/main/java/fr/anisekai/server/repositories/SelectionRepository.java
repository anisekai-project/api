package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Selection;
import org.springframework.stereotype.Repository;

@Repository
public interface SelectionRepository extends AnisekaiRepository<Selection, Long> {

}
