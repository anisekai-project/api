package fr.anisekai.discord.responses.messages;

import fr.anisekai.discord.interfaces.MessageRequestResponse;
import fr.anisekai.discord.responses.embeds.AnimeCardEmbed;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public class AnimeCardMessage implements MessageRequestResponse {

    private final Anime                anime;
    private final Collection<Interest> interests;
    private final Role                 role;

    public AnimeCardMessage(Anime anime) {

        this(anime, Collections.emptyList(), null);
    }

    public AnimeCardMessage(Anime anime, Collection<Interest> interests) {

        this(anime, interests, null);
    }

    public AnimeCardMessage(Anime anime, Collection<Interest> interests, Role role) {

        this.anime     = anime;
        this.interests = interests;
        this.role      = role;
    }

    /**
     * Retrieve the {@link MessageRequest} {@link Consumer} that should set the response content.
     *
     * @return A {@link MessageRequest} {@link Consumer}
     */
    @Override
    public Consumer<MessageRequest<?>> getHandler() {

        return mr -> {
            AnimeCardEmbed animeCard = new AnimeCardEmbed();
            animeCard.setAnime(this.anime);
            animeCard.setInterests(this.interests);

            if (this.role != null) {
                ActionRow buttons = ActionRow.of(
                        Button.success(this.getButtonUrl(2), "Je suis intéressé(e)"),
                        Button.secondary(this.getButtonUrl(0), "Je suis neutre"),
                        Button.danger(this.getButtonUrl(-2), "Je ne suis pas intéressé(e)")
                );

                mr.setContent(String.format("Hey %s ! Un anime a été ajouté !", this.role.getAsMention()))
                  .setAllowedMentions(Collections.singletonList(Message.MentionType.ROLE))
                  .setComponents(buttons);
            }

            mr.setEmbeds(animeCard.build());
        };
    }

    private String getButtonUrl(int level) {

        return String.format("button://interest?anime=%s&interest=%s", this.anime.getId(), level);
    }

}
