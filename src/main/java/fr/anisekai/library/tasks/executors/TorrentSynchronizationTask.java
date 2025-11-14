package fr.anisekai.library.tasks.executors;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.library.services.SpringTransmissionClient;
import fr.anisekai.library.tasks.factories.MediaImportFactory;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.TorrentService;
import fr.anisekai.server.tasking.TaskExecutor;

import java.util.List;
import java.util.Optional;

public class TorrentSynchronizationTask implements TaskExecutor {

    private final TaskService    taskService;
    private final TorrentService torrentService;

    public TorrentSynchronizationTask(TaskService taskService, TorrentService torrentService) {

        this.taskService    = taskService;
        this.torrentService = torrentService;
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) throws Exception {

        timer.action("client-check", "Check the transmission client");
        SpringTransmissionClient client = this.torrentService.getClient();
        client.check();
        if (!client.isAvailable()) throw new IllegalArgumentException("Transmission client is not available");
        timer.endAction();

        timer.action("load-torrents", "Load torrents");
        timer.action("remote", "Load remote torrents");
        List<Transmission.Torrent> torrents = this.torrentService.getClient().query();
        timer.endAction();

        timer.action("local", "Load local torrents");
        List<Torrent> downloading = this.torrentService.getAllDownloading();
        timer.endAction();
        timer.endAction();

        timer.action("sync-torrents", "Load remote torrents");
        for (Torrent downloadingTorrent : downloading) {
            timer.action("sync-torrent", "Sync torrent " + downloadingTorrent.getId());

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

                if (transmissionTorrent.status().isFinished()) {
                    timer.action("submit-task", "Submit media importation task");
                    this.taskService.getFactory(MediaImportFactory.class).queue(downloadingTorrent);
                    timer.endAction();
                }
            } else {
                this.torrentService.mod(
                        downloadingTorrent.getId(),
                        entity -> entity.setStatus(Transmission.TorrentStatus.UNKNOWN)
                );
            }

            timer.endAction();
        }
        timer.endAction();
    }

}
