package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.domain.keys.TorrentKey;
import fr.anisekai.server.repositories.TorrentFileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class TorrentFileService {

    private final TorrentFileRepository repository;

    public TorrentFileService(TorrentFileRepository repository) {

        this.repository = repository;
    }

    /**
     * @deprecated Transition method, prefer declaring dedicated methods.
     */
    @Deprecated
    public TorrentFile mod(TorrentKey id, Consumer<TorrentFile> updater) {

        return this.repository.mod(id, updater);
    }

    /**
     * @return The entity.
     */
    @Deprecated
    public TorrentFile requireById(TorrentKey id) {

        return this.repository.requireById(id);
    }

    public TorrentFileRepository getRepository() {

        return this.repository;
    }

    public List<TorrentFile> getFiles(Torrent torrent) {

        return this.repository.findAllByTorrent(torrent);
    }

    public Optional<TorrentFile> getFile(Episode episode) {

        return this.repository.findByEpisode(episode);
    }

}
