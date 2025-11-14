package fr.anisekai.discord.responses.embeds.selections;

import fr.anisekai.Texts;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import fr.anisekai.server.domain.enums.SelectionStatus;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Comparator;
import java.util.stream.Collectors;

public class SelectionClosedEmbed extends EmbedBuilder {

    public SelectionClosedEmbed(Selection selection, Iterable<Voter> voters) {

        this.setTitle(Texts.formatted(selection.getSeason(), selection.getYear()), "https://anichart.net/");

        if (selection.getStatus() == SelectionStatus.AUTO_CLOSED) {
            this.setDescription("Les votes pour cette saison n'ont pas été nécessaire !");
            this.appendDescription("\n\n");

            String animes = selection.getAnimes().stream()
                                     .sorted(Comparator.comparing(Anime::getId))
                                     .map(anime -> String.format("- [%s](%s)", anime.getTitle(), anime.getUrl()))
                                     .collect(Collectors.joining("\n"));

            this.appendDescription(animes);
            return;
        }

        this.setDescription("Les votes pour cette saison sont terminés !");
        this.appendDescription("\n\n");

        for (Voter voter : voters) {
            DiscordUser user = voter.getUser();

            String username = String.format("- %s <@%s>", user.getEmote(), user.getId());
            String votes = voter.getVotes()
                                .stream()
                                .sorted(Comparator.comparing(Anime::getId))
                                .map(anime -> String.format("  - [%s](%s)", anime.getTitle(), anime.getUrl()))
                                .collect(Collectors.joining("\n"));

            this.appendDescription(username).appendDescription("\n").appendDescription(votes).appendDescription("\n\n");
        }
    }

}
