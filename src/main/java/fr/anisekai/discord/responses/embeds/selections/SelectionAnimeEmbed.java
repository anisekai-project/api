package fr.anisekai.discord.responses.embeds.selections;

import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.utils.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class SelectionAnimeEmbed extends EmbedBuilder {

    public SelectionAnimeEmbed(Selection selection, Map<Anime, DiscordUser> votes) {


        String animes = selection.getAnimes().stream()
                                 .sorted(Comparator.comparing(Anime::getId))
                                 .map(anime -> {
                                     if (votes.containsKey(anime)) {
                                         DiscordUser voter = votes.get(anime);
                                         return String.format(
                                                 "%s — %s [%s](%s)",
                                                 this.padded(anime.getId(), "—"),
                                                 voter.getEmote(),
                                                 StringUtils.truncate(anime.getTitle(), 50),
                                                 anime.getUrl()
                                         );
                                     } else {
                                         return String.format(
                                                 "**%s — [%s](%s)**",
                                                 this.padded(anime.getId()),
                                                 StringUtils.truncate(anime.getTitle(), 50),
                                                 anime.getUrl()
                                         );
                                     }
                                 })
                                 .collect(Collectors.joining("\n"));

        this.setDescription(animes);
    }

    private String padded(long id) {

        return this.padded(id, " ");
    }

    private String padded(long id, String padder) {

        int    len   = 4;
        String value = String.valueOf(id);
        int    pad   = len - value.length();

        return "`%s%s`".formatted(padder.repeat(pad), value);
    }

}
