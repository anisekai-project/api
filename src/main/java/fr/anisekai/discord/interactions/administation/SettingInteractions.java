package fr.anisekai.discord.interactions.administation;

import fr.alexpado.jda.interactions.annotations.Interact;
import fr.alexpado.jda.interactions.annotations.Option;
import fr.alexpado.jda.interactions.annotations.Param;
import fr.alexpado.jda.interactions.enums.SlashTarget;
import fr.alexpado.jda.interactions.responses.SlashResponse;
import fr.anisekai.discord.annotations.InteractionBean;
import fr.anisekai.discord.exceptions.RequireAdministratorException;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.services.SettingService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;

@Component
@InteractionBean
public class SettingInteractions {

    private final SettingService service;

    public SettingInteractions(SettingService service) {

        this.service = service;
    }

    private static void requireAdministrator(DiscordUser user) {

        if (!user.isAdministrator()) {
            throw new RequireAdministratorException();
        }
    }

    // <editor-fold desc="@ setting/watchlist-channel ─ Set the channel that will be used for watchlists. [channel: Channel]">
    @Interact(
            name = "setting/watchlist-channel",
            description = "\uD83D\uDD12 — Défini le salon qui sera utilisé pour les listes de visionnage.",
            target = SlashTarget.GUILD,
            options = {
                    @Option(
                            name = "channel",
                            description = "Le salon a définir pour cette option",
                            type = OptionType.CHANNEL,
                            required = true
                    )
            }
    )
    public SlashResponse settingWatchlistChannel(DiscordUser user, @Param("channel") Channel channel) {

        requireAdministrator(user);

        if (channel.getType() == ChannelType.TEXT) {
            this.service.setSetting(SettingService.WATCHLIST_CHANNEL, channel.getId());
            return DiscordResponse.info("Les listes d'anime seront envoyées dans %s.", channel.getAsMention());
        }

        return DiscordResponse.error("Merci de choisir un channel textuel pour envoyer les listes.");
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/announcement-role ─ Set the role that will be mentioned for anime announcements. [channel: Channel]">
    @Interact(
            name = "setting/announcement-role",
            description = "\uD83D\uDD12 — Défini le role qui sera utilisé pour les annonces.",
            target = SlashTarget.GUILD,
            options = {
                    @Option(
                            name = "role",
                            description = "Le role a définir pour cette option",
                            type = OptionType.ROLE,
                            required = true
                    )
            }
    )
    public SlashResponse settingAnnouncementRole(DiscordUser user, @Param("role") Role role) {

        requireAdministrator(user);

        this.service.setSetting(SettingService.ANNOUNCEMENT_ROLE, role.getId());
        return DiscordResponse.info("Le role %s sera utilisé pour les annonces d'anime.", role.getAsMention());
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/announcement-channel ─ Set the channel that will be used for anime announcements. [channel: Channel]">
    @Interact(
            name = "setting/announcement-channel",
            description = "\uD83D\uDD12 — Défini le salon qui sera utilisé pour les annonces.",
            target = SlashTarget.GUILD,
            options = {
                    @Option(
                            name = "channel",
                            description = "Le salon a définir pour cette option",
                            type = OptionType.CHANNEL,
                            required = true
                    )
            }
    )
    public SlashResponse settingAnnouncementChannel(DiscordUser user, @Param("channel") Channel channel) {

        requireAdministrator(user);

        if (channel.getType() == ChannelType.TEXT) {
            this.service.setSetting(SettingService.ANNOUNCEMENT_CHANNEL, channel.getId());
            return DiscordResponse.info("Les annonces d'anime seront envoyées dans %s.", channel.getAsMention());
        }

        return DiscordResponse.error("Merci de choisir un channel textuel pour envoyer les annonces.");
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/audit-channel ─ Set the channel that will be used for log messages. [channel: ?Channel]">
    @Interact(
            name = "setting/audit-channel",
            description = "\uD83D\uDD12 — Défini le salon qui sera utilisé pour les messages d'administration.",
            target = SlashTarget.GUILD,
            options = {
                    @Option(
                            name = "channel",
                            description = "Le salon a définir pour cette option",
                            type = OptionType.CHANNEL,
                            required = true
                    )
            }
    )
    public SlashResponse settingAuditChannel(DiscordUser user, @Param("channel") Channel channel) {

        requireAdministrator(user);

        if (channel.getType() == ChannelType.TEXT) {
            this.service.setSetting(SettingService.AUDIT_CHANNEL, channel.getId());
            return DiscordResponse.info(
                    "Les messages d'administration seront envoyés dans %s.",
                    channel.getAsMention()
            );
        }

        return DiscordResponse.error("Merci de choisir un channel textuel pour envoyer les messages d'administration.");
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/enable-auto-download ─ Enable or disable automated downloads. [value: boolean]">
    @Interact(
            name = "setting/enable-auto-download",
            description = "\uD83D\uDD12 — Active ou désactive le téléchargement automatique des épisodes.",
            target = SlashTarget.ALL,
            options = {
                    @Option(
                            name = "value",
                            description = "Valeur de l'option",
                            type = OptionType.BOOLEAN,
                            required = true
                    )
            }
    )
    public SlashResponse settingAutoDownload(DiscordUser user, @Param("value") boolean value) {

        requireAdministrator(user);

        this.service.setSetting(SettingService.DOWNLOAD_ENABLED, Boolean.toString(value));
        return DiscordResponse.info("Les téléchargements automatiques ont été %s.", value ? "activés" : "désactivés");
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/download-server ─ Set the URL pointing to the download server. [value: string]">
    @Interact(
            name = "setting/download-server",
            description = "\uD83D\uDD12 — Défini le serveur de téléchargement (Transmission).",
            target = SlashTarget.ALL,
            options = {
                    @Option(
                            name = "value",
                            description = "URL du serveur",
                            type = OptionType.STRING,
                            required = true
                    )
            }
    )
    public SlashResponse settingDownloadServer(DiscordUser user, @Param("value") String value) {

        requireAdministrator(user);

        this.service.setSetting(SettingService.DOWNLOAD_SERVER, value);
        return DiscordResponse.info("Les téléchargements automatiques seront effectué via `%s`.", value);
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/download-source ─ Set the URL pointing to the download source. [value: string]">
    @Interact(
            name = "setting/download-source",
            description = "\uD83D\uDD12 — Défini le lien pour la source de téléchargement (Torrent).",
            target = SlashTarget.ALL,
            options = {
                    @Option(
                            name = "value",
                            description = "URL RSS Torrent",
                            type = OptionType.STRING,
                            required = true
                    )
            }
    )
    public SlashResponse settingDownloadSource(DiscordUser user, @Param("value") String value) {

        requireAdministrator(user);

        this.service.setSetting(SettingService.DOWNLOAD_SOURCE, value);
        return DiscordResponse.info("La source de téléchargement a été défini sur `%s`.", value);
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/enable-anime-announcement ─ Enable or disable announcement when an anime is imported. [value: boolean]">
    @Interact(
            name = "setting/enable-anime-announcement",
            description = "\uD83D\uDD12 — Active ou désactive les annonces automatique des animes.",
            target = SlashTarget.ALL,
            options = {
                    @Option(
                            name = "value",
                            description = "Valeur de l'option",
                            type = OptionType.BOOLEAN,
                            required = true
                    )
            }
    )
    public SlashResponse settingAnimeAnnouncements(DiscordUser user, @Param("value") boolean value) {

        requireAdministrator(user);

        this.service.setSetting(SettingService.ANIME_AUTO_ANNOUNCE, Boolean.toString(value));
        return DiscordResponse.info("Les annonces automatiques ont été %s.", value ? "activés" : "désactivés");
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/server ─ Define the active server to the current one">
    @Interact(
            name = "setting/server",
            description = "\uD83D\uDD12 — Défini le serveur actif sur le serveur actuel.",
            target = SlashTarget.ALL
    )
    public SlashResponse settingServer(DiscordUser user, Guild guild) {

        requireAdministrator(user);

        this.service.setSetting(SettingService.SERVER_ID, guild.getId());
        return DiscordResponse.info("Le serveur actif a bien été défini.");
    }
    // </editor-fold>

    // <editor-fold desc="@ setting/download-retention ─ Set the amount of day to wait to delete a torrent. [value: integer]">
    @Interact(
            name = "setting/download-retention",
            description = "\uD83D\uDD12 — Défini le nombre de jour avant la suppression d'un torrent.",
            target = SlashTarget.ALL,
            options = {
                    @Option(
                            name = "value",
                            description = "Nombre de jour (0 pour infini)",
                            type = OptionType.INTEGER,
                            required = true
                    )
            }
    )
    public SlashResponse settingDownloadRetention(DiscordUser user, @Param("value") long value) {

        requireAdministrator(user);

        if (value < 0 || value > 300) {
            return DiscordResponse.error("Merci de rentrer une valeur comprise entre 0 (inclus) et 300 (inclus)");
        }

        this.service.setSetting(SettingService.DOWNLOAD_RETENTION, String.valueOf(value));

        if (value == 0) {
            return DiscordResponse.info("Les torrents ne seront jamais supprimés.");
        }

        return DiscordResponse.info("Les torrents seront gardés pour un maximum de **%s** jours.", value);
    }
    // </editor-fold>

}
