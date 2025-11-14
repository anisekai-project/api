package fr.anisekai.discord.interactions.administation;


import fr.alexpado.jda.interactions.annotations.Interact;
import fr.alexpado.jda.interactions.annotations.Option;
import fr.alexpado.jda.interactions.annotations.Param;
import fr.alexpado.jda.interactions.responses.ButtonResponse;
import fr.alexpado.jda.interactions.responses.SlashResponse;
import fr.anisekai.discord.annotations.InteractAt;
import fr.anisekai.discord.annotations.InteractionBean;
import fr.anisekai.discord.exceptions.RequireAdministratorException;
import fr.anisekai.discord.responses.messages.SelectionMessage;
import fr.anisekai.discord.utils.InteractionType;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import fr.anisekai.server.domain.enums.SelectionStatus;
import fr.anisekai.server.services.SelectionService;
import fr.anisekai.server.services.VoterService;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@InteractionBean
public class SelectionInteractions {

    private final SelectionService service;
    private final VoterService     voterService;

    public SelectionInteractions(SelectionService service, VoterService voterService) {

        this.service      = service;
        this.voterService = voterService;
    }

    private static void requireAdministrator(DiscordUser user) {

        if (!user.isAdministrator()) {
            throw new RequireAdministratorException();
        }
    }

    // <editor-fold desc="@ selection/create ─ Start an anime selection for the next season.">
    @Interact(
            name = "selection/create",
            description = "\uD83D\uDD12 — Démarre une séléction d'anime pour la prochaine saison.",
            options = {
                    @Option(
                            name = "votes",
                            description = "Nombre de vote au total pour la selection. (Par défaut: 8)",
                            type = OptionType.INTEGER
                    )
            }
    )
    public SlashResponse createSelection(DiscordUser user, @Param("votes") Long votesParam) {

        requireAdministrator(user);

        long        votes     = Optional.ofNullable(votesParam).orElse(8L);
        Selection   selection = this.service.createSelection(votes);
        List<Voter> voters    = this.voterService.createVoters(selection, votes);

        return new SelectionMessage(selection, voters);
    }
    // </editor-fold>

    // <editor-fold desc="█ selection/close ─ Close the votes for a selection [selection: integer]">
    @Interact(
            name = "selection/close",
            description = "\uD83D\uDD12 — Ferme les votes pour la selection en cours.",
            options = {
                    @Option(
                            name = "selection",
                            description = "",
                            type = OptionType.INTEGER,
                            required = true
                    )
            }
    )
    @InteractAt(InteractionType.BUTTON)
    public ButtonResponse closeSelection(DiscordUser user, @Param("selection") long selectionId) {

        requireAdministrator(user);

        Selection selection = this.service.mod(
                selectionId,
                entity -> entity.setStatus(SelectionStatus.CLOSED)
        );

        List<Voter> voters = this.voterService.getVoters(selection);

        return new SelectionMessage(selection, voters);
    }
    // </editor-fold>

}
