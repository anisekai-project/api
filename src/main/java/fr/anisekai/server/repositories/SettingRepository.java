package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Setting;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends AnisekaiRepository<Setting, String> {

}
