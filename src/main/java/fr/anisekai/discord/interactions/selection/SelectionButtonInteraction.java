package fr.anisekai.discord.interactions.selection;

import fr.alexpado.interactions.annotations.Button;
import fr.alexpado.interactions.annotations.Param;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.messages.SelectionMessage;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import fr.anisekai.server.domain.enums.SelectionStatus;
import fr.anisekai.server.services.SelectionService;
import fr.anisekai.server.services.VoterService;
import fr.anisekai.utils.UuidCodec;

import java.util.List;
import java.util.UUID;

@DiscordBean
public class SelectionButtonInteraction {

    private final SelectionService service;
    private final VoterService     voterService;

    public SelectionButtonInteraction(SelectionService service, VoterService voterService) {

        this.service      = service;
        this.voterService = voterService;
    }

    public static String of(Selection selection) {

        return "button://selection/close?selection=" + UuidCodec.encode(selection.getId());
    }

    @Button(name = "selection/close")
    @RequireAdmin
    public InteractionResponse execute(@Param("selection") String selectionId) {

        UUID id = UuidCodec.decode(selectionId);
        Selection selection = this.service.mod(
                id,
                entity -> entity.setStatus(SelectionStatus.CLOSED)
        );

        List<Voter> voters = this.voterService.getVoters(selection);

        return new SelectionMessage(selection, voters);
    }

}
