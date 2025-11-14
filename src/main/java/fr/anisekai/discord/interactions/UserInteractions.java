package fr.anisekai.discord.interactions;

import fr.alexpado.jda.interactions.annotations.Interact;
import fr.alexpado.jda.interactions.annotations.Option;
import fr.alexpado.jda.interactions.annotations.Param;
import fr.alexpado.jda.interactions.responses.ButtonResponse;
import fr.alexpado.jda.interactions.responses.SlashResponse;
import fr.anisekai.discord.annotations.InteractAt;
import fr.anisekai.discord.annotations.InteractionBean;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.discord.responses.messages.AnimeCardMessage;
import fr.anisekai.discord.responses.messages.ProfileMessage;
import fr.anisekai.discord.responses.messages.SelectionMessage;
import fr.anisekai.discord.utils.InteractionType;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.services.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@InteractionBean
public class UserInteractions {

    private final AnimeService     animeService;
    private final UserService      userService;
    private final InterestService  interestService;
    private final SelectionService selectionService;
    private final VoterService     voterService;

    public UserInteractions(AnimeService animeService, UserService userService, InterestService interestService, SelectionService selectionService, VoterService voterService) {

        this.animeService     = animeService;
        this.userService      = userService;
        this.interestService  = interestService;
        this.selectionService = selectionService;
        this.voterService     = voterService;
    }

    // <editor-fold desc="@ card ─ Displays an anime card message [anime: integer]">
    @Interact(
            name = "anime-card",
            description = "Permet de visionner la fiche d'un anime.",
            options = {
                    @Option(
                            autoComplete = true,
                            description = "Anime pour lequel la fiche sera envoyée.",
                            name = "anime",
                            required = true,
                            type = OptionType.INTEGER
                    )
            }
    )
    public SlashResponse viewAnime(@Param("anime") long animeId) {

        Anime          anime     = this.animeService.requireById(animeId);
        List<Interest> interests = this.interestService.getInterests(anime);
        return new AnimeCardMessage(anime, interests);
    }
    // </editor-fold>

    // <editor-fold desc="@ emote ─ Change the vote emote [icon: string]">
    @Interact(
            name = "emote",
            description = "Change l'emote de vote.",
            options = {
                    @Option(
                            name = "emote",
                            description = "Emote de vote",
                            type = OptionType.STRING,
                            required = true
                    ),
            }
    )
    public SlashResponse changeEmote(DiscordUser user, @Param("emote") String emote) {

        if (!this.userService.canUseEmote(user, emote)) {
            return DiscordResponse.error("L'emote choisie est déjà utilisée.");
        }

        this.userService.useEmote(user, emote);
        return DiscordResponse.success("L'emote de vote a été mise à jour.");
    }
    // </editor-fold>

    // <editor-fold desc="% interest ─ Change the interest level on an anime. [anime: integer, interest: integer]">
    @Interact(
            name = "interest",
            description = "Change l'intérêt porté à un anime.",
            options = {
                    @Option(
                            autoComplete = true,
                            description = "Anime pour lequel la fiche sera envoyée.",
                            name = "anime",
                            required = true,
                            type = OptionType.INTEGER
                    ),
                    @Option(
                            // TODO Add to autocomplete
                            name = "interest",
                            description = "Le niveau d'intérêt",
                            required = true,
                            type = OptionType.INTEGER,
                            autoComplete = true
                    )
            }
    )
    public SlashResponse animeInterest(DiscordUser user, @Param("anime") long animeId, @Param("interest") long interestLevel) {

        if (user.getEmote() == null) {
            return DiscordResponse.privateError(
                    "Vous devez définir une emote de vote avant de pouvoir choisir votre intérêt pour un anime.");
        }

        Anime anime = this.animeService.requireById(animeId);

        if (interestLevel < -2 || interestLevel > 2) {
            return DiscordResponse.error("La valeur d'intérêt doit être comprise entre -2 (inclus) et 2 (inclus)");
        }

        byte level = (byte) interestLevel;

        this.interestService.setInterest(user, anime, level);
        return DiscordResponse.privateSuccess("Le niveau d'intérêt a bien été mis à jour.");
    }
    // </editor-fold>

    // <editor-fold desc="@ profile ─ Display a user profile. [user: Member]">
    @Interact(
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
    public SlashResponse profileView(User sender, @Param("user") Member member) {

        User           effectiveUser        = member == null ? sender : member.getUser();
        DiscordUser    effectiveDiscordUser = this.userService.of(effectiveUser);
        List<Anime>    animes               = this.animeService.getRepository().findByAddedBy(effectiveDiscordUser);
        List<Interest> interests            = this.interestService.getInterests(effectiveDiscordUser);

        return new ProfileMessage(effectiveUser, effectiveDiscordUser, animes, interests);
    }
    // </editor-fold>

    // <editor-fold desc="█ vote ─ Cast a vote on a selection [selection: integer, anime: integer]">
    @Interact(
            name = "vote",
            description = "Vote pour un anime d'une selection.",
            options = {
                    @Option(
                            name = "selection",
                            description = "",
                            type = OptionType.INTEGER,
                            required = true
                    ),
                    @Option(
                            name = "anime",
                            description = "",
                            type = OptionType.INTEGER,
                            required = true
                    )
            }
    )
    @InteractAt(InteractionType.BUTTON)
    public ButtonResponse castSelectionVote(DiscordUser user, @Param("selection") long selectionId, @Param("anime") long animeId) {

        Selection selection = this.selectionService.requireById(selectionId);
        Anime     anime     = this.animeService.requireById(animeId);

        this.voterService.castVote(selection, user, anime);
        return new SelectionMessage(selection, this.voterService.getVoters(selection));
    }
    // </editor-fold>

}
