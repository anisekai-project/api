package fr.anisekai.discord.interactions.setting;

import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.services.SettingService;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;

@DiscordBean
@RequireAdmin
public class AnnouncementSettingSlashInteraction {

    private final SettingService service;

    public AnnouncementSettingSlashInteraction(SettingService service) {

        this.service = service;
    }

    @Slash(
            name = "setting/announcement/enabled",
            description = "\uD83D\uDD12 — Active ou désactive les annonces automatique des animes.",
            options = @Option(
                    name = "value",
                    description = "Valeur de l'option",
                    type = OptionType.BOOLEAN,
                    required = true
            )
    )
    public InteractionResponse executeAnnouncementEnabled(@Param("state") boolean value) {

        this.service.setSetting(SettingService.ANIME_AUTO_ANNOUNCE, Boolean.toString(value));
        return DiscordResponse.info("Les annonces automatiques ont été %s.", value ? "activés" : "désactivés");
    }

    @Slash(
            name = "setting/announcement/channel",
            description = "\uD83D\uDD12 — Défini le salon qui sera utilisé pour les annonces.",
            options = @Option(
                    name = "channel",
                    description = "Le salon a définir pour cette option",
                    type = OptionType.CHANNEL,
                    required = true
            )
    )
    public InteractionResponse settingAnnouncementChannel(@Param("channel") Channel channel) {

        if (channel.getType() == ChannelType.TEXT) {
            this.service.setSetting(SettingService.ANNOUNCEMENT_CHANNEL, channel.getId());
            return DiscordResponse.info("Les annonces d'anime seront envoyées dans %s.", channel.getAsMention());
        }

        return DiscordResponse.error("Merci de choisir un channel textuel pour envoyer les annonces.");
    }

    @Slash(
            name = "setting/announcement/role",
            description = "\uD83D\uDD12 — Défini le role qui sera utilisé pour les annonces.",
            options = @Option(
                    name = "role",
                    description = "Le role a définir pour cette option",
                    type = OptionType.ROLE,
                    required = true
            )
    )
    public InteractionResponse settingAnnouncementRole(@Param("role") Role role) {

        this.service.setSetting(SettingService.ANNOUNCEMENT_ROLE, role.getId());
        return DiscordResponse.info("Le role %s sera utilisé pour les annonces d'anime.", role.getAsMention());
    }


}
