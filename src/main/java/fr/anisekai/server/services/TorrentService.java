package fr.anisekai.server.services;

import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.library.services.SpringTransmissionClient;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.repositories.TorrentRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class TorrentService {

    private final TorrentRepository repository;
    private final SpringTransmissionClient client;

    public TorrentService(TorrentRepository repository, SpringTransmissionClient client) {

        this.repository = repository;
        this.client     = client;
    }

    public TorrentRepository getRepository() {

        return this.repository;
    }

    /**
     * @deprecated Transition method, prefer declaring dedicated methods.
     */
    @Deprecated
    public Torrent mod(UUID id, Consumer<Torrent> updater) {

        return this.repository.mod(id, updater);
    }

    /**
     * @param id
     *         The entity identifier.
     *
     * @return The entity.
     */
    @Deprecated
    public Torrent requireById(UUID id) {

        return this.repository.requireById(id);
    }

    public SpringTransmissionClient getClient() {

        return this.client;
    }

    public List<Torrent> getAllDownloading() {

        List<Transmission.TorrentStatus> statuses = Arrays.stream(Transmission.TorrentStatus.values())
                                                          .filter(status -> !status.isFinished())
                                                          .filter(status -> status != Transmission.TorrentStatus.UNKNOWN)
                                                          .toList();

        return this.repository.findByStatusIn(statuses);
    }

    public List<Torrent> getAllFinishedBefore(ZonedDateTime start) {

        List<Transmission.TorrentStatus> statuses = Arrays.stream(Transmission.TorrentStatus.values())
                                                          .filter(Transmission.TorrentStatus::isFinished)
                                                          .toList();

        return this.repository.findByStatusInAndUpdatedAtLessThan(statuses, start.toInstant());
    }

}
