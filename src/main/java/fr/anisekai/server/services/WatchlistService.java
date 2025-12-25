package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Watchlist;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.repositories.WatchlistRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistRepository repository;

    public WatchlistService(WatchlistRepository repository) {

        this.repository = repository;
    }

    public WatchlistRepository getRepository() {

        return this.repository;
    }

    /**
     * @deprecated Transition method, prefer declaring dedicated methods.
     */
    @Deprecated
    public Watchlist mod(AnimeList id, Consumer<Watchlist> updater) {

        return this.repository.mod(id, updater);
    }

    /**
     * @param id
     *         The entity identifier.
     *
     * @return The entity.
     */
    @Deprecated
    public Watchlist requireById(AnimeList id) {

        return this.repository.requireById(id);
    }

    /**
     * Create a {@link Watchlist} for the provided {@link AnimeList}.
     *
     * @param list
     *         The {@link AnimeList} from which the {@link Watchlist} will be created.
     *
     * @return A {@link Watchlist}
     */
    private Watchlist create(AnimeList list) {

        Watchlist watchlist = new Watchlist();
        watchlist.setId(list);
        return this.repository.save(watchlist);
    }

    /**
     * Create all {@link Watchlist} matching any {@link AnimeList} having the {@link AnimeList.Property#SHOW} property,
     * in the order they have been declared in the enum.
     *
     * @return A {@link List} of all created {@link Watchlist}.
     */
    public List<Watchlist> create() {

        List<Watchlist> all = this.repository.findAll();

        if (!all.isEmpty()) {
            throw new IllegalStateException("You cannot use create() when there are existing watchlists");
        }

        return AnimeList.collect(AnimeList.Property.SHOW).stream()
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .map(this::create)
                        .toList();
    }

    /**
     * Delete all {@link Watchlist} and re-create them using {@link #create()}.
     * <p>
     * <b>Note:</b> Corresponding Discord message ({@link Watchlist#getMessageId()}) will not be deleted.
     *
     * @return A {@link List} of all created {@link Watchlist}.
     */
    public List<Watchlist> reset() {

        this.repository.deleteAll();
        return this.create();
    }

}
