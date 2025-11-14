package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Watchlist;
import fr.anisekai.server.domain.enums.AnimeList;

public interface WatchlistRepository extends AnisekaiRepository<Watchlist, AnimeList> {

}
