package fr.anisekai.discord.interactions.setting;

import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.services.SettingService;
import net.dv8tion.jda.api.interactions.commands.OptionType;

@DiscordBean
@RequireAdmin
public class TorrentSettingSlashInteraction {

    private final SettingService service;

    public TorrentSettingSlashInteraction(SettingService service) {

        this.service = service;
    }

    @Slash(
            name = "setting/downloads/enabled",
            description = "\uD83D\uDD12 — Active ou désactive le téléchargement automatique des épisodes.",
            options = @Option(
                    name = "state",
                    description = "Status du téléchargement automatique.",
                    type = OptionType.BOOLEAN,
                    required = true
            )
    )
    public InteractionResponse executeDownloadEnabled(@Param("state") boolean value) {

        this.service.setSetting(SettingService.DOWNLOAD_ENABLED, Boolean.toString(value));
        return DiscordResponse.info("Les téléchargements automatiques ont été %s.", value ? "activés" : "désactivés");
    }

    @Slash(
            name = "setting/downloads/client",
            description = "\uD83D\uDD12 — Défini le serveur de téléchargement",
            options = @Option(
                    name = "url",
                    description = "URL du serveur.",
                    type = OptionType.STRING,
                    required = true
            )
    )
    public InteractionResponse executeDownloadClient(@Param("url") String url) {

        // TODO: Try to create a transmission client with the provided url to check for connectivity.
        this.service.setSetting(SettingService.DOWNLOAD_SERVER, url);
        return DiscordResponse.info("Les téléchargements automatiques seront effectué via `%s`.", url);
    }

    @Slash(
            name = "setting/downloads/source",
            description = "\uD83D\uDD12 — Défini le lien par défaut pour la source de téléchargement",
            options = @Option(
                    name = "url",
                    description = "URL du flux RSS.",
                    type = OptionType.STRING,
                    required = true
            )
    )
    public InteractionResponse executeDownloadSource(@Param("url") String url) {

        // TODO: Try to create an rss client to check for connectivity.
        this.service.setSetting(SettingService.DOWNLOAD_SOURCE, url);
        return DiscordResponse.info("La source de téléchargement a été défini sur `%s`.", url);
    }

    @Slash(
            name = "setting/downloads/retention",
            description = "\uD83D\uDD12 — Défini le nombre de jour avant la suppression d'un torrent.",
            options = @Option(
                    name = "duration",
                    description = "Nombre de jour (0 pour infini)",
                    type = OptionType.STRING,
                    required = true,
                    minInt = 0
            )
    )
    public InteractionResponse executeDownloadRetention(@Param("duration") long duration) {

        this.service.setSetting(SettingService.DOWNLOAD_RETENTION, String.valueOf(duration));

        if (duration == 0) {
            return DiscordResponse.info("Les torrents ne seront jamais supprimés.");
        }

        return DiscordResponse.info("Les torrents seront gardés pour un maximum de **%s** jours.", duration);
    }

}
