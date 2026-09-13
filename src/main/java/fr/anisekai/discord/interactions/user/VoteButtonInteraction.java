package fr.anisekai.discord.interactions.user;

import fr.alexpado.interactions.annotations.Button;
import fr.alexpado.interactions.annotations.Param;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.messages.SelectionMessage;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.SelectionService;
import fr.anisekai.server.services.VoterService;
import fr.anisekai.utils.UuidCodec;

import java.util.UUID;

@DiscordBean
public class VoteButtonInteraction {

    private final AnimeService     animeService;
    private final SelectionService selectionService;
    private final VoterService     voterService;
    public VoteButtonInteraction(AnimeService animeService, SelectionService selectionService, VoterService voterService) {

        this.animeService     = animeService;
        this.selectionService = selectionService;
        this.voterService     = voterService;
    }

    public static String of(Selection selection, Anime anime) {

        return "button://vote?selection=%s&anime=%s".formatted(
                UuidCodec.encode(selection.getId()),
                UuidCodec.encode(anime.getId())
        );
    }

    @Button(name = "vote")
    public InteractionResponse execute(DiscordUser user, @Param("selection") String selectionId, @Param("anime") String animeId) {

        UUID sid = UuidCodec.decode(selectionId);
        UUID aid = UuidCodec.decode(animeId);

        Selection selection = this.selectionService.requireById(sid);
        Anime     anime     = this.animeService.requireById(aid);

        this.voterService.castVote(selection, user, anime);
        return new SelectionMessage(selection, this.voterService.getVoters(selection));
    }

}
