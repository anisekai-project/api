package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Setting;
import fr.anisekai.server.repositories.SettingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SettingService {

    public static final String WATCHLIST_CHANNEL    = "discord.channels.watchlist";
    public static final String ANNOUNCEMENT_CHANNEL = "discord.channels.announcements";
    public static final String AUDIT_CHANNEL        = "discord.channels.audit";
    public static final String ANNOUNCEMENT_ROLE    = "discord.roles.audit";
    public static final String SERVER_ID            = "discord.server";
    public static final String DOWNLOAD_ENABLED     = "application.downloads.enabled";
    public static final String DOWNLOAD_SERVER      = "application.downloads.server";
    public static final String DOWNLOAD_SOURCE      = "application.downloads.source";
    public static final String DOWNLOAD_RETENTION   = "application.downloads.retention";
    public static final String ANIME_AUTO_ANNOUNCE  = "application.announces.anime";

    private final SettingRepository repository;

    public SettingService(SettingRepository repository) {

        this.repository = repository;
    }

    private Optional<String> getSetting(String id) {

        return this.repository
                .findById(id)
                .map(Setting::getValue);
    }

    public void setSetting(String id, String value) {

        this.repository.upsert(
                () -> this.repository.findById(id),
                () -> {
                    Setting setting = new Setting();
                    setting.setId(id);
                    return setting;
                },
                setting -> setting.setValue(value)
        );
    }

    // <editor-fold desc="General Getters">

    public Optional<Long> getWatchlistChannel() {

        return this.getSetting(WATCHLIST_CHANNEL).map(Long::parseLong);
    }

    public Optional<Long> getAnnouncementChannel() {

        return this.getSetting(ANNOUNCEMENT_CHANNEL).map(Long::parseLong);
    }

    public Optional<Long> getAuditChannel() {

        return this.getSetting(AUDIT_CHANNEL).map(Long::parseLong);
    }

    public Optional<Long> getAnnouncementRole() {

        return this.getSetting(ANNOUNCEMENT_ROLE).map(Long::parseLong);
    }

    public Optional<Long> getServerId() {

        return this.getSetting(SERVER_ID).map(Long::parseLong);
    }

    public boolean isDownloadEnabled() {

        return this.getSetting(DOWNLOAD_ENABLED)
                   .map(Boolean::parseBoolean)
                   .orElse(false);
    }

    public boolean isAnimeAnnouncementEnabled() {

        return this.getSetting(ANIME_AUTO_ANNOUNCE)
                   .map(Boolean::parseBoolean)
                   .orElse(false);
    }

    public Optional<String> getDownloadServer() {

        return this.getSetting(DOWNLOAD_SERVER);
    }

    public Optional<String> getDownloadSource() {

        return this.getSetting(DOWNLOAD_SOURCE);
    }

    public Optional<Long> getDownloadRetention() {

        return this.getSetting(DOWNLOAD_RETENTION).map(Long::parseLong);
    }

    // </editor-fold>
}
