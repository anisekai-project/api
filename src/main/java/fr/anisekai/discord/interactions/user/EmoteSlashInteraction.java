package fr.anisekai.discord.interactions.user;

import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.interfaces.MessageRequestResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.services.UserService;
import net.dv8tion.jda.api.interactions.commands.OptionType;

@DiscordBean
public class EmoteSlashInteraction {

    private final UserService service;

    public EmoteSlashInteraction(UserService service) {

        this.service = service;
    }

    @Slash(
            name = "emote",
            description = "Change l'emote de vote.",
            options = {
                    @Option(
                            name = "emote",
                            description = "Emote de vote",
                            type = OptionType.STRING,
                            required = true
                    )
            }
    )
    public MessageRequestResponse execute(DiscordUser user, @Param("emote") String emote) {

        if (!this.service.canUseEmote(user, emote)) {
            return DiscordResponse.error("L'emote choisie est déjà utilisée.");
        }

        this.service.useEmote(user, emote);
        return DiscordResponse.success("L'emote de vote a été mise à jour.");
    }

}
