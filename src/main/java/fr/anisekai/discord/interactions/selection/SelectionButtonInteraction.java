package fr.anisekai.discord.interactions.selection;

import fr.alexpado.interactions.annotations.Button;
import fr.alexpado.interactions.annotations.Param;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.interfaces.MessageRequestResponse;
import fr.anisekai.discord.responses.messages.SelectionMessage;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import fr.anisekai.server.domain.enums.SelectionStatus;
import fr.anisekai.server.services.SelectionService;
import fr.anisekai.server.services.VoterService;

import java.util.List;

@DiscordBean
public class SelectionButtonInteraction {

    private final SelectionService service;
    private final VoterService     voterService;

    public SelectionButtonInteraction(SelectionService service, VoterService voterService) {

        this.service      = service;
        this.voterService = voterService;
    }

    @Button(name = "interest")
    public MessageRequestResponse execute(DiscordUser user, @Param("selection") long selectionId) {

        Selection selection = this.service.mod(
                selectionId,
                entity -> entity.setStatus(SelectionStatus.CLOSED)
        );

        List<Voter> voters = this.voterService.getVoters(selection);

        return new SelectionMessage(selection, voters);
    }

}
