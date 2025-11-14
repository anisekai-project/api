package fr.anisekai.discord.responses.embeds;

import fr.anisekai.Texts;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.utils.DiscordUtils;
import fr.anisekai.utils.EntityUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.ZonedDateTime;
import java.util.*;

public class WatchlistEmbed extends EmbedBuilder {

    private static final String UP_VOTE   = "▲ %s";
    private static final String DOWN_VOTE = "▼ %s";
    private static final String ALL_VOTE  = "▲ %s — ▼ %s";

    private static final String FULL_NP = "%s\n> %s";
    private static final String FULL_WP = "%s — %s/%s\n> %s";
    private static final String MINI_WP = "%s — %s/%s";

    private final Collection<String> full       = new ArrayList<>();
    private final Collection<String> fullNoLink = new ArrayList<>();
    private final Collection<String> mini       = new ArrayList<>();
    private final Collection<String> miniNoLink = new ArrayList<>();

    public void setWatchlistContent(AnimeList list, List<Anime> animes, Collection<Interest> interests) {

        String watchlistName = String.format("%s (%s)", Texts.formatted(list), animes.size());

        this.setAuthor(watchlistName);
        this.setTimestamp(ZonedDateTime.now());

        if (animes.isEmpty()) {
            this.setDescription("*Aucun anime présent dans cette liste...*");
            return;
        }

        animes.stream()
              .sorted(Comparator.comparing(Anime::getTitle))
              .forEach(anime -> {
                  List<Interest> animeInterests = interests
                          .stream()
                          .filter(interest -> EntityUtils.equals(interest.getAnime(), anime))
                          .filter(interest -> !Objects.isNull(interest.getUser().getEmote()))
                          .sorted(Comparator.comparing(interest -> interest.getUser().getUsername()))
                          .toList();

                  this.submitAnime(anime, animeInterests, list.hasProperty(AnimeList.Property.PROGRESS));
              });

        String fullContent       = String.join("\n\n", this.full);
        String fullNoLinkContent = String.join("\n\n", this.fullNoLink);
        String miniContent       = String.join("\n", this.mini);
        String miniNoLinkContent = String.join("\n", this.miniNoLink);

        if (fullContent.length() < MessageEmbed.DESCRIPTION_MAX_LENGTH) {
            this.setDescription(fullContent);
        } else if (fullNoLinkContent.length() < MessageEmbed.DESCRIPTION_MAX_LENGTH) {
            this.setDescription(fullNoLinkContent);
        } else if (miniContent.length() < MessageEmbed.DESCRIPTION_MAX_LENGTH) {
            this.setDescription(miniContent);
        } else if (miniNoLinkContent.length() < MessageEmbed.DESCRIPTION_MAX_LENGTH) {
            this.setDescription(miniNoLinkContent);
        } else {
            this.setDescription("*Malheureusement, il y a trop de contenu dans cette liste* :(");
        }
    }

    private void submitAnime(Anime anime, Collection<Interest> interests, boolean showProgress) {

        String title       = anime.getTitle();
        String linkedTitle = DiscordUtils.link(anime);
        long   watched     = anime.getWatched();
        String total       = anime.getTotal() < 0 ? "*%s*".formatted(anime.getTotal() * -1) : "**%s**".formatted(anime.getTotal());

        List<String> positiveInterests = interests
                .stream()
                .filter(interest -> interest.getLevel() > 0)
                .map(Interest::getUser)
                .map(DiscordUser::getEmote)
                .toList();

        List<String> negativeInterests = interests
                .stream()
                .filter(interest -> interest.getLevel() < 0)
                .map(Interest::getUser)
                .map(DiscordUser::getEmote)
                .toList();

        String votes = "*Aucun vote*";

        if (!positiveInterests.isEmpty() && !negativeInterests.isEmpty()) {
            votes = String.format(
                    ALL_VOTE,
                    String.join("", positiveInterests),
                    String.join("", negativeInterests)
            );
        } else if (!positiveInterests.isEmpty()) {
            votes = String.format(
                    UP_VOTE,
                    String.join("", positiveInterests)
            );
        } else if (!negativeInterests.isEmpty()) {
            votes = String.format(
                    DOWN_VOTE,
                    String.join("", negativeInterests)
            );
        }

        if (showProgress) {
            this.full.add(FULL_WP.formatted(linkedTitle, watched, total, votes));
            this.fullNoLink.add(FULL_WP.formatted(title, watched, total, votes));
            this.mini.add(MINI_WP.formatted(linkedTitle, watched, total));
            this.miniNoLink.add(MINI_WP.formatted(title, watched, total));
        } else {
            this.full.add(FULL_NP.formatted(linkedTitle, votes));
            this.fullNoLink.add(FULL_NP.formatted(title, votes));
            this.mini.add(linkedTitle);
            this.miniNoLink.add(title);
        }

    }


}
