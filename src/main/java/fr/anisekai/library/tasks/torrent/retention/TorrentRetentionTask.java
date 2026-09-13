package fr.anisekai.library.tasks.torrent.retention;

import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.library.Library;
import fr.anisekai.library.services.SpringTransmissionClient;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.server.services.TorrentService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TorrentRetentionTask implements TaskHandler<TorrentRetentionInput, Nothing> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentRetentionTask.class);

    private final Library            library;
    private final TorrentService     torrentService;
    private final TorrentFileService torrentFileService;

    public TorrentRetentionTask(Library library, TorrentService torrentService, TorrentFileService torrentFileService) {

        this.library            = library;
        this.torrentService     = torrentService;
        this.torrentFileService = torrentFileService;
    }

    @Override
    public @NonNull Nothing handle(@NonNull TorrentRetentionInput arguments) throws Exception {

        if (arguments.retentionDuration().isZero()) {
            return Nothing.INSTANCE;
        }

        ZonedDateTime limit    = ZonedDateTime.now().minus(arguments.retentionDuration());
        List<Torrent> torrents = this.torrentService.getAllFinishedBefore(limit);

        for (Torrent torrent : torrents) {
            Set<TorrentFile> files = torrent.getFiles();

            LOGGER.info("Purging torrent {} ({} files) ...", torrent.getId(), files.size());

            for (TorrentFile file : files) {
                if (file.isRemoved()) continue;

                Optional<Path> optionalFile = this.library.findDownload(file);

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

        return Nothing.INSTANCE;
    }

}
