package fr.anisekai.discord.interactions.user;

import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.messages.ProfileMessage;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.UserService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.List;

@DiscordBean
public class ProfileSlashInteraction {

    private final AnimeService    animeService;
    private final InterestService interestService;
    private final UserService     userService;

    public ProfileSlashInteraction(AnimeService animeService, InterestService interestService, UserService userService) {

        this.animeService    = animeService;
        this.interestService = interestService;
        this.userService     = userService;
    }

    @Slash(
            name = "profile",
            description = "Afficher le profil utilisateur.",
            options = {
                    @Option(
                            name = "user",
                            description = "Utilisateur pour lequel sera affiché le profil. (Par défaut: vous)",
                            type = OptionType.USER
                    )
            }
    )
    public InteractionResponse execute(User sender, @Param("user") Member member) {

        User           effectiveUser        = member == null ? sender : member.getUser();
        DiscordUser    effectiveDiscordUser = this.userService.of(effectiveUser);
        List<Anime>    animes               = this.animeService.getRepository().findByAddedBy(effectiveDiscordUser);
        List<Interest> interests            = this.interestService.getInterests(effectiveDiscordUser);

        return new ProfileMessage(effectiveUser, effectiveDiscordUser, animes, interests);
    }

}
