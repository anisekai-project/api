package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.enums.AnimeList;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnimeRepository extends AnisekaiRepository<Anime, Long> {

    List<Anime> findByAddedBy(DiscordUser addedBy);

    List<Anime> findAllByList(AnimeList animeStatus);

    List<Anime> findAllByListIn(Collection<AnimeList> animeStatuses);

    List<Anime> findAllByTitleRegexIsNotNull();

    Optional<Anime> findByUrl(String url);

    @Query("""
                select a from Anime a
                where exists (
                    select 1 from Episode e
                    where e.anime = a and e.ready = true
                )
            """)
    List<Anime> findByEpisodeReady();


}
