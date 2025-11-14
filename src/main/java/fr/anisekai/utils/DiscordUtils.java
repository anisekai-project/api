package fr.anisekai.utils;

import fr.anisekai.Texts;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Watchlist;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Utility class containing various method for Discord formatting
 */
public final class DiscordUtils {

    private DiscordUtils() {}

    /**
     * Creates a Markdown link with the provided name and link.
     *
     * @param name
     *         The display name for the Markdown link
     * @param link
     *         The target for the Markdown link
     *
     * @return A Markdown link as string.
     */
    public static String link(String name, String link) {

        return "[%s](%s)".formatted(name, link);
    }

    /**
     * Creates a Markdown link for the {@link Anime} provided.
     *
     * @param anime
     *         The {@link Anime} for which the link will be created.
     *
     * @return A Markdown link as string.
     */
    public static String link(Anime anime) {

        return link(anime.getTitle(), anime.getUrl());
    }

    /**
     * Creates a basic {@link EmbedBuilder} based on the {@link ApplicationEvent} provided.
     *
     * @param event
     *         The {@link ApplicationEvent}
     *
     * @return An {@link EmbedBuilder} with a footer and a timestamp already defined using the {@link ApplicationEvent}
     *         provided.
     */
    public static EmbedBuilder event(ApplicationEvent event) {

        return new EmbedBuilder()
                .setFooter(event.getSource().getClass().getName())
                .setTimestamp(OffsetDateTime.now());
    }

    /**
     * Create a command choice with the provided id and name. If the name is longer than 100 characters, the string will
     * be cut and '...' will be appended.
     *
     * @param id
     *         The choice's id
     * @param name
     *         The choice's name
     *
     * @return A command choice
     */
    public static Command.Choice asChoice(long id, String name) {

        if (name.length() > 100) {
            return new Command.Choice(String.format("%s...", name.substring(0, 90)), id);
        }
        return new Command.Choice(name, id);
    }

    /**
     * Transform the provided {@link Selection} into a command choice.
     *
     * @param selection
     *         The {@link Selection} to transform
     *
     * @return A command choice
     */
    public static Command.Choice asChoice(Selection selection) {

        return asChoice(selection.getId(), Texts.formatted(selection.getSeason(), selection.getYear()));
    }

    public static Optional<Message> findExistingMessage(MessageChannel channel, Anime anime) {

        if (anime.getAnnouncementId() == null || anime.getAnnouncementId() == -1) {
            return Optional.empty();
        }

        return findExistingMessage(channel, anime.getAnnouncementId());
    }

    public static Optional<Message> findExistingMessage(MessageChannel channel, Watchlist watchlist) {

        if (watchlist.getMessageId() == null) {
            return Optional.empty();
        }
        return findExistingMessage(channel, watchlist.getMessageId());
    }

    public static Optional<Message> findExistingMessage(MessageChannel channel, long messageId) {

        try {
            return Optional.of(channel.retrieveMessageById(messageId).complete());
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public static @NotNull ScheduledEvent requireEvent(Guild guild, Broadcast broadcast) {

        if (broadcast.getEventId() == null) {
            throw new IllegalStateException("Broadcast is not scheduled on Discord.");
        }

        ScheduledEvent event = guild.getScheduledEventById(broadcast.getEventId());

        if (event == null) {
            throw new IllegalStateException("Broadcast is not scheduled on Discord.");
        }
        return event;
    }

}
