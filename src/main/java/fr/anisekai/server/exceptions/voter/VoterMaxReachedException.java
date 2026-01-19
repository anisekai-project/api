package fr.anisekai.server.exceptions.voter;

import fr.anisekai.discord.interfaces.DiscordEmbeddable;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class VoterMaxReachedException extends RuntimeException implements DiscordEmbeddable {

    public VoterMaxReachedException() {

        super("Can't cast vote: This user has reached the maximum number of votes.");
    }

    /**
     * Retrieve an {@link EmbedBuilder} representing this {@link DiscordEmbeddable}.
     *
     * @return An {@link EmbedBuilder}.
     */
    @Override
    public EmbedBuilder asEmbed() {

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Impossible de voter.");
        embed.setDescription("Tu as atteint le maximum de vote autorisé.");
        embed.setColor(Color.ORANGE);

        return embed;
    }

}
