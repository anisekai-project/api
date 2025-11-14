package fr.anisekai.discord.responses.embeds.selections;

import fr.anisekai.Texts;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Collection;
import java.util.stream.Collectors;

public class SelectionVoterEmbed extends EmbedBuilder {

    public SelectionVoterEmbed(Selection selection, Collection<Voter> voters) {

        this.setTitle(Texts.formatted(selection.getSeason(), selection.getYear()), "https://anichart.net/");
        this.setDescription("Les votes pour cette saison sont ouverts !");
        this.appendDescription("\n\n");

        String participants = voters.stream()
                                    .map(voter -> String.format(
                                            "【 %s 】— %s vote(s) (<@%s>)",
                                            voter.getUser().getEmote(),
                                            voter.getAmount(),
                                            voter.getUser().getId()
                                    ))
                                    .collect(Collectors.joining("\n"));

        this.appendDescription(participants);

    }

}
