package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.DiscordUser;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends AnisekaiRepository<DiscordUser, Long> {

    List<DiscordUser> findAllByActiveIsTrue();

}
