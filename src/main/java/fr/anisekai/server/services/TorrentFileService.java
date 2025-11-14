package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.domain.keys.TorrentKey;
import fr.anisekai.server.repositories.TorrentFileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TorrentFileService extends AnisekaiService<TorrentFile, TorrentKey, TorrentFileRepository> {

    public TorrentFileService(TorrentFileRepository repository, EntityEventProcessor eventProcessor) {

        super(repository, eventProcessor);
    }

    public List<TorrentFile> getFiles(Torrent torrent) {

        return this.getRepository().findAllByTorrent(torrent);
    }

    public Optional<TorrentFile> getFile(Episode episode) {

        return this.getRepository().findByEpisode(episode);
    }

}
