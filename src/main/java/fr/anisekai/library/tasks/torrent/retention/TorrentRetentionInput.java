package fr.anisekai.library.tasks.torrent.retention;

import java.time.Duration;

public record TorrentRetentionInput(
        Duration retentionDuration
) {
}
