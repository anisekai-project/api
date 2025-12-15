package fr.anisekai.discord.interactions.selection;

import fr.alexpado.interactions.annotations.Deferrable;
import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.interfaces.MessageRequestResponse;
import fr.anisekai.discord.responses.messages.SelectionMessage;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import fr.anisekai.server.services.SelectionService;
import fr.anisekai.server.services.VoterService;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.List;
import java.util.Optional;

@DiscordBean
@RequireAdmin
public class SelectionSlashInteraction {

    private final SelectionService service;
    private final VoterService     voterService;

    public SelectionSlashInteraction(SelectionService service, VoterService voterService) {

        this.service      = service;
        this.voterService = voterService;
    }


    @Slash(
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
    @Deferrable
    public MessageRequestResponse executeSchedule(@Param("votes") Long votesParam) {

        long        votes     = Optional.ofNullable(votesParam).orElse(8L);
        Selection   selection = this.service.createSelection(votes);
        List<Voter> voters    = this.voterService.createVoters(selection, votes);

        return new SelectionMessage(selection, voters);
    }

}
