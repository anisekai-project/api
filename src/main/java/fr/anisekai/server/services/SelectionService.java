package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.enums.AnimeSeason;
import fr.anisekai.server.domain.enums.SelectionStatus;
import fr.anisekai.server.repositories.SelectionRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

@Service
public class SelectionService {

    private static final long DEFAULT_TOTAL_VOTE = 8;

    private final SelectionRepository repository;
    private final AnimeService        animeService;

    public SelectionService(SelectionRepository repository, AnimeService animeService) {

        this.repository = repository;
        this.animeService = animeService;
    }

    /**
     * @deprecated Transition method, prefer declaring dedicated methods.
     */
    @Deprecated
    public Selection mod(long id, Consumer<Selection> updater) {

        return this.repository.mod(id, updater);
    }

    /**
     * @param id
     *         The entity identifier.
     *
     * @return The entity.
     */
    @Deprecated
    public Selection requireById(long id) {

        return this.repository.requireById(id);
    }

    public Selection createSelection() {

        return this.createSelection(DEFAULT_TOTAL_VOTE);
    }

    public Selection createSelection(long amount) {

        // Retrieving data necessary for the selection
        List<Anime> simulcasts = this.animeService.getSimulcastsAvailable();

        // Adding one month here ensure we fall under the right season and year for the selection, hopefully.
        // ... like if I will ever be early anyway...
        ZonedDateTime now    = ZonedDateTime.now().plusMonths(1);
        AnimeSeason   season = AnimeSeason.fromDate(now);

        Selection entity = new Selection();
        entity.setSeason(season);
        entity.setStatus(simulcasts.size() <= amount ? SelectionStatus.AUTO_CLOSED : SelectionStatus.OPEN);
        entity.setYear(now.getYear());
        entity.setAnimes(new HashSet<>(simulcasts));

        return this.repository.save(entity);
    }

}
