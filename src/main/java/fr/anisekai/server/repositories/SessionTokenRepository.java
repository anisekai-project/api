package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.enums.TokenType;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionTokenRepository extends AnisekaiRepository<SessionToken, UUID> {

    Optional<SessionToken> findByIdAndTypeIn(UUID id, Collection<TokenType> type);

}
