package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.domain.keys.TorrentKey;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TorrentFileRepository extends AnisekaiRepository<TorrentFile, TorrentKey> {

    List<TorrentFile> findAllByTorrent(Torrent torrent);

    Optional<TorrentFile> findByEpisode(Episode episode);

}
