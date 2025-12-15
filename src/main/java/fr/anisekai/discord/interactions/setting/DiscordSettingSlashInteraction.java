package fr.anisekai.discord.interactions.setting;

import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.interfaces.MessageRequestResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.services.SettingService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;

@DiscordBean
@RequireAdmin
public class DiscordSettingSlashInteraction {

    private final SettingService service;

    public DiscordSettingSlashInteraction(SettingService service) {

        this.service = service;
    }

    @Slash(
            name = "setting/discord/server",
            description = "\uD83D\uDD12 — Active ou désactive le téléchargement automatique des épisodes."
    )
    public MessageRequestResponse executeDiscordServer(Guild guild) {

        this.service.setSetting(SettingService.SERVER_ID, guild.getId());
        return DiscordResponse.info("Le serveur actif a bien été défini.");
    }

    @Slash(
            name = "setting/discord/audit",
            description = "\uD83D\uDD12 — Défini le salon qui sera utilisé pour les messages d'administration.",
            options = @Option(
                    name = "channel",
                    description = "Le salon a définir pour cette option",
                    type = OptionType.CHANNEL,
                    required = true
            )
    )
    public MessageRequestResponse executeDiscordAudit(@Param("channel") Channel channel) {

        if (channel.getType() == ChannelType.TEXT) {
            this.service.setSetting(SettingService.AUDIT_CHANNEL, channel.getId());
            return DiscordResponse.info(
                    "Les messages d'administration seront envoyés dans %s.",
                    channel.getAsMention()
            );
        }

        return DiscordResponse.error("Merci de choisir un channel textuel pour envoyer les messages d'administration.");
    }

    @Slash(
            name = "setting/discord/watchlist",
            description = "\uD83D\uDD12 — Défini le salon qui sera utilisé pour les listes de visionnage.",
            options = @Option(
                    name = "channel",
                    description = "Le salon a définir pour cette option",
                    type = OptionType.CHANNEL,
                    required = true
            )
    )
    public MessageRequestResponse executeDiscordWatchlist(@Param("channel") Channel channel) {

        if (channel.getType() == ChannelType.TEXT) {
            this.service.setSetting(SettingService.WATCHLIST_CHANNEL, channel.getId());
            return DiscordResponse.info(
                    "Les listes de visionnage seront envoyés dans %s.",
                    channel.getAsMention()
            );
        }

        return DiscordResponse.error("Merci de choisir un channel textuel pour envoyer les listes de visionnage.");
    }

}
