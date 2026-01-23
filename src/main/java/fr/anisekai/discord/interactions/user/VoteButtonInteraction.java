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

@DiscordBean
public class VoteButtonInteraction {

    public static String of(Selection selection, Anime anime) {

        return "button://vote?selection=%s&anime=%s".formatted(selection.getId(), anime.getId());
    }

    private final AnimeService     animeService;
    private final SelectionService selectionService;
    private final VoterService     voterService;

    public VoteButtonInteraction(AnimeService animeService, SelectionService selectionService, VoterService voterService) {

        this.animeService     = animeService;
        this.selectionService = selectionService;
        this.voterService     = voterService;
    }

    @Button(name = "vote")
    public InteractionResponse execute(DiscordUser user, @Param("selection") long selectionId, @Param("anime") long animeId) {

        Selection selection = this.selectionService.requireById(selectionId);
        Anime     anime     = this.animeService.requireById(animeId);

        this.voterService.castVote(selection, user, anime);
        return new SelectionMessage(selection, this.voterService.getVoters(selection));
    }

}
