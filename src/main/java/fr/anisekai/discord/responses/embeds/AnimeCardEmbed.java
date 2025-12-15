package fr.anisekai.discord.responses.embeds;

import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.stream.Collectors;

public class AnimeCardEmbed extends EmbedBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setAnime(Anime anime) {

        String episodeText;

        if (anime.getTotal() == 0) {
            episodeText = "*Inconnu*";
        } else if (anime.getTotal() < 0) {
            episodeText = String.format("%s *(Estimation)*", anime.getTotal() * -1);
        } else {
            episodeText = String.valueOf(anime.getTotal());
        }

        this.setTitle(anime.getTitle(), anime.getUrl());
        this.setThumbnail(anime.getThumbnailUrl());
        this.setDescription(anime.getSynopsis());

        this.addField("Nombre d'épisodes", episodeText, false);
        this.addField("Tags", String.join(", ", anime.getTags()), false);
    }

    public void setInterests(Collection<Interest> interests) {

        if (interests.isEmpty()) {
            return;
        }

        String positiveIcons = interests.stream()
                                        .filter(interest -> interest.getLevel() > 0)
                                        .map(Interest::getUser)
                                        .map(user -> String.format("%s <@%s>", user.getEmote(), user.getId()))
                                        .collect(Collectors.joining("\n"));

        String negativeIcons = interests.stream()
                                        .filter(interest -> interest.getLevel() < 0)
                                        .map(Interest::getUser)
                                        .map(user -> String.format("%s <@%s>", user.getEmote(), user.getId()))
                                        .collect(Collectors.joining("\n"));

        this.addField("Personne(s) intéressée(s)", positiveIcons, true);
        this.addField("Personne(s) non intéressée(s)", negativeIcons, true);
        this.addBlankField(true);

    }

}
