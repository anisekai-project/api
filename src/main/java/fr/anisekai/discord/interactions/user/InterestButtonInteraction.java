package fr.anisekai.discord.interactions.user;

import fr.alexpado.interactions.annotations.Button;
import fr.alexpado.interactions.annotations.Param;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.interfaces.MessageRequestResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;

@DiscordBean
public class InterestButtonInteraction {

    private final InterestService interestService;

    public static String of(Selection selection, Anime anime) {

        return "button://interest?selection=%s&anime=%s".formatted(selection.getId(), anime.getId());
    }

    private final AnimeService animeService;


    public InterestButtonInteraction(AnimeService animeService, InterestService interestService) {

        this.animeService    = animeService;
        this.interestService = interestService;
    }

    @Button(name = "interest")
    public MessageRequestResponse execute(DiscordUser user, @Param("anime") long animeId, @Param("interest") long interest) {

        if (user.getEmote() == null) {
            return DiscordResponse.privateError(
                    "Vous devez définir une emote de vote avant de pouvoir choisir votre intérêt pour un anime.");
        }

        Anime anime = this.animeService.requireById(animeId);

        if (interest < -2 || interest > 2) {
            return DiscordResponse.error("La valeur d'intérêt doit être comprise entre -2 (inclus) et 2 (inclus)");
        }

        byte level = (byte) interest;

        this.interestService.setInterest(user, anime, level);
        return DiscordResponse.privateSuccess("Le niveau d'intérêt a bien été mis à jour.");
    }

}
