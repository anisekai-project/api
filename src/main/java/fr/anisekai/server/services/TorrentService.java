package fr.anisekai.server.services;

import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.library.services.SpringTransmissionClient;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.repositories.TorrentRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class TorrentService extends AnisekaiService<Torrent, UUID, TorrentRepository> {

    private final SpringTransmissionClient client;

    public TorrentService(TorrentRepository repository, EntityEventProcessor eventProcessor, SpringTransmissionClient client) {

        super(repository, eventProcessor);
        this.client = client;
    }

    public SpringTransmissionClient getClient() {

        return this.client;
    }

    public List<Torrent> getAllDownloading() {

        List<Transmission.TorrentStatus> statuses = Arrays.stream(Transmission.TorrentStatus.values())
                                                          .filter(status -> !status.isFinished())
                                                          .filter(status -> status != Transmission.TorrentStatus.UNKNOWN)
                                                          .toList();

        return this.getRepository().findByStatusIn(statuses);
    }

    public List<Torrent> getAllFinishedBefore(ZonedDateTime start) {

        List<Transmission.TorrentStatus> statuses = Arrays.stream(Transmission.TorrentStatus.values())
                                                          .filter(Transmission.TorrentStatus::isFinished)
                                                          .toList();

        return this.getRepository().findByStatusInAndUpdatedAtLessThan(statuses, start);
    }

}
