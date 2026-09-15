package fr.anisekai;

import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.library.tasks.torrent.retention.TorrentRetentionInput;
import fr.anisekai.library.tasks.torrent.retention.TorrentRetentionTaskFactory;
import fr.anisekai.library.tasks.torrent.sourcing.TorrentSourcingTaskFactory;
import fr.anisekai.library.tasks.torrent.sourcing.TorrentSourcingTaskInput;
import fr.anisekai.library.tasks.torrent.synchronization.TorrentSynchronizationTaskFactory;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.SettingService;
import fr.anisekai.server.services.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class ApplicationRepeatedActions {

    private final static Logger LOGGER = LoggerFactory.getLogger(ApplicationRepeatedActions.class);

    private final TaskService    service;
    private final SettingService settingService;

    public ApplicationRepeatedActions(TaskService service, SettingService settingService) {

        this.service        = service;
        this.settingService = settingService;
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void runCleaning() {

        Optional<Long> downloadRetention = this.settingService.getDownloadRetention();

        if (downloadRetention.isEmpty()) return;
        long days = downloadRetention.get();
        if (days == 0) return;

        Duration              retention = Duration.ofDays(days);
        TorrentRetentionInput input     = new TorrentRetentionInput(retention);
        this.service.queueOne(TorrentRetentionTaskFactory.class, input, Task.PRIORITY_AUTOMATIC_LOW);
    }

    @Scheduled(cron = "0/1 * * * * *")
    public void runTorrentSync() {

        if (!this.settingService.isDownloadEnabled()) {
            return;
        }

        Optional<String> optionalServer = this.settingService.getDownloadServer();

        if (optionalServer.isEmpty()) {
            return;
        }

        if (!this.service.hasScheduled("torrent:synchronize")) {
            this.service.queueOne(
                    TorrentSynchronizationTaskFactory.class,
                    Nothing.INSTANCE,
                    Task.PRIORITY_AUTOMATIC_LOW
            );
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    private void runTorrentSourcing() {

        if (!this.settingService.isDownloadEnabled()) {
            return;
        }

        Optional<String> optionalSource = this.settingService.getDownloadSource();
        Optional<String> optionalServer = this.settingService.getDownloadServer();

        if (optionalSource.isEmpty()) {
            LOGGER.warn("No download source available");
            return;
        }

        if (optionalServer.isEmpty()) {
            LOGGER.warn("No download server available");
            return;
        }

        String                   source = optionalSource.get();
        TorrentSourcingTaskInput input  = new TorrentSourcingTaskInput(source, Task.PRIORITY_AUTOMATIC_LOW);
        this.service.queueOne(TorrentSourcingTaskFactory.class, input, Task.PRIORITY_AUTOMATIC_LOW);
    }

}
