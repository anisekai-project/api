package fr.anisekai.discord.interfaces;

import net.dv8tion.jda.api.EmbedBuilder;

public interface DiscordEmbeddable {

    /**
     * Retrieve an {@link EmbedBuilder} representing this {@link DiscordEmbeddable}.
     *
     * @return An {@link EmbedBuilder}.
     */
    EmbedBuilder asEmbed();

}
