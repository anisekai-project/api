package fr.anisekai.discord.responses.messages;

import fr.anisekai.discord.interfaces.ButtonResponse;
import fr.anisekai.discord.responses.embeds.selections.SelectionAnimeEmbed;
import fr.anisekai.discord.responses.embeds.selections.SelectionClosedEmbed;
import fr.anisekai.discord.responses.embeds.selections.SelectionVoterEmbed;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.*;
import java.util.function.Consumer;

public class SelectionMessage implements ButtonResponse {

    private final Selection         selection;
    private final Collection<Voter> voters;

    public SelectionMessage(Selection selection, Collection<Voter> voters) {

        this.selection = selection;
        this.voters    = voters;
    }

    /**
     * Retrieve the {@link MessageRequest} {@link Consumer} that should set the response content.
     *
     * @return A {@link MessageRequest} {@link Consumer}
     */
    @Override
    public Consumer<MessageRequest<?>> getHandler() {

        return mr -> {
            Collection<MessageEmbed> embeds = new ArrayList<>();

            if (this.selection.getStatus().isClosed()) {
                EmbedBuilder closedEmbed = new SelectionClosedEmbed(this.selection, this.voters);
                embeds.add(closedEmbed.build());

                mr.setEmbeds(embeds);
                mr.setComponents(Collections.emptyList());
            } else {
                Map<Anime, DiscordUser> votes = new HashMap<>();

                for (Voter voter : this.voters) {
                    voter.getVotes().forEach(anime -> votes.put(anime, voter.getUser()));
                }

                EmbedBuilder voterEmbed = new SelectionVoterEmbed(this.selection, this.voters);
                EmbedBuilder animeEmbed = new SelectionAnimeEmbed(this.selection, votes);

                embeds.add(voterEmbed.build());
                embeds.add(animeEmbed.build());

                List<Button> buttons = this.selection.getAnimes()
                                                     .stream()
                                                     .sorted(Comparator.comparingLong(Anime::getId))
                                                     .map(anime -> votes.containsKey(anime) ? this.asButton(
                                                             anime,
                                                             votes.get(anime)
                                                     ) : this.asButton(anime))
                                                     .toList();

                Collection<Button> allButtons = new ArrayList<>(buttons);

                allButtons.add(Button.of(
                        ButtonStyle.DANGER,
                        String.format("button://selection/close?selection=%s", this.selection.getId()),
                        "Clôturer"
                ));
                mr.setEmbeds(embeds);
                mr.setComponents(ActionRow.partitionOf(allButtons));
            }
        };
    }

    private Button asButton(Anime anime) {

        return Button.primary(
                String.format("button://vote?selection=%s&anime=%s", this.selection.getId(), anime.getId()),
                String.valueOf(anime.getId())
        );
    }

    private Button asButton(Anime anime, DiscordUser votedBy) {

        return Button.of(
                ButtonStyle.SECONDARY,
                String.format("button://vote?selection=%s&anime=%s", this.selection.getId(), anime.getId()),
                String.valueOf(anime.getId()),
                Emoji.fromUnicode(Objects.requireNonNull(votedBy.getEmote()))
        );

    }

}
