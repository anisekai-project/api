package fr.anisekai.library.tasks.executors;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.library.Library;
import fr.anisekai.library.services.SpringTransmissionClient;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.services.SettingService;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.server.services.TorrentService;
import fr.anisekai.server.tasking.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TorrentRetentionControlTask implements TaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentRetentionControlTask.class);

    private final SettingService     settingService;
    private final TorrentService     torrentService;
    private final TorrentFileService torrentFileService;
    private final Library            libraryService;

    public TorrentRetentionControlTask(SettingService settingService, TorrentService torrentService, TorrentFileService torrentFileService, Library libraryService) {

        this.settingService     = settingService;
        this.torrentService     = torrentService;
        this.torrentFileService = torrentFileService;
        this.libraryService     = libraryService;
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) throws Exception {

        long retention = this.settingService.getDownloadRetention().orElse(0L);
        if (retention == 0) return;

        ZonedDateTime limit    = ZonedDateTime.now().minusDays(retention);
        List<Torrent> torrents = this.torrentService.getAllFinishedBefore(limit);

        for (Torrent torrent : torrents) {
            Set<TorrentFile> files = torrent.getFiles();

            LOGGER.info("Purging torrent {} ({} files) ...", torrent.getId(), files.size());

            for (TorrentFile file : files) {
                if (file.isRemoved()) continue;

                Optional<Path> optionalFile = this.libraryService.findDownload(file);

                if (optionalFile.isEmpty()) {
                    LOGGER.debug("Unable to find file {}", file.getIndex());
                    continue;
                }

                Files.delete(optionalFile.get());

                LOGGER.debug("File {} removed", file.getIndex());
                this.torrentFileService.mod(file.getId(), entity -> entity.setRemoved(true));
                file.setRemoved(true);
            }

            if (torrent.getFiles().stream().allMatch(TorrentFile::isRemoved)) {
                SpringTransmissionClient client = this.torrentService.getClient();
                if (client.isAvailable()) {
                    client.delete(torrent.asTransmissionIdentifier());
                    this.torrentService.mod(
                            torrent.getId(),
                            entity -> entity.setStatus(Transmission.TorrentStatus.UNKNOWN)
                    );
                }
            }
        }
    }

}
