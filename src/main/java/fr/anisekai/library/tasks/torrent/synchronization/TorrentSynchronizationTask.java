package fr.anisekai.library.tasks.torrent.synchronization;

import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.library.services.SpringTransmissionClient;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.services.TorrentService;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public class TorrentSynchronizationTask implements TaskHandler<Nothing, Nothing> {

    private final TorrentService torrentService;

    public TorrentSynchronizationTask(TorrentService torrentService) {

        this.torrentService = torrentService;
    }

    @Override
    public @NonNull Nothing handle(@NonNull Nothing arguments) throws Exception {

        SpringTransmissionClient client = this.torrentService.getClient();
        client.check();
        if (!client.isAvailable()) throw new IllegalArgumentException("Transmission client is not available");

        List<Transmission.Torrent> torrents    = this.torrentService.getClient().query();
        List<Torrent>              downloading = this.torrentService.getAllDownloading();

        for (Torrent downloadingTorrent : downloading) {
            // Find a matching torrent in the download list.
            Optional<Transmission.Torrent> optionalTransmissionTorrent = torrents
                    .stream()
                    .filter(item -> item.hash().equals(downloadingTorrent.getHash()))
                    .findFirst();

            if (optionalTransmissionTorrent.isPresent()) {
                Transmission.Torrent transmissionTorrent = optionalTransmissionTorrent.get();
                this.torrentService.mod(
                        downloadingTorrent.getId(),
                        entity -> {
                            entity.setProgress(transmissionTorrent.percentDone());
                            entity.setStatus(transmissionTorrent.status());
                        }
                );
            } else {
                this.torrentService.mod(
                        downloadingTorrent.getId(),
                        entity -> entity.setStatus(Transmission.TorrentStatus.UNKNOWN)
                );
            }
        }
        return Nothing.INSTANCE;
    }

}
