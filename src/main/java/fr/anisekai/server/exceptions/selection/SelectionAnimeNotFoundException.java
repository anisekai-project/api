package fr.anisekai.server.exceptions.selection;

import fr.anisekai.discord.interfaces.DiscordEmbeddable;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class SelectionAnimeNotFoundException extends RuntimeException implements DiscordEmbeddable {

    /**
     * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and
     * may subsequently be initialized by a call to {@link #initCause}.
     */
    public SelectionAnimeNotFoundException() {

        super("The anime requested is not part of the selection.");
    }

    /**
     * Retrieve an {@link EmbedBuilder} representing this {@link DiscordEmbeddable}.
     *
     * @return An {@link EmbedBuilder}.
     */
    @Override
    public EmbedBuilder asEmbed() {

        return new EmbedBuilder().setDescription("Cet anime ne fait pas parti de la sélection.").setColor(Color.RED);
    }

}
