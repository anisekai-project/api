package fr.anisekai.discord.exceptions;

import fr.anisekai.discord.interfaces.DiscordEmbeddable;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class RequireAdministratorException extends RuntimeException implements DiscordEmbeddable {

    /**
     * Retrieve an {@link EmbedBuilder} representing this {@link DiscordEmbeddable}.
     *
     * @return An {@link EmbedBuilder}.
     */
    @Override
    public EmbedBuilder asEmbed() {

        return new EmbedBuilder()
                .setTitle("Désolé, mais tu ne peux pas faire ça.")
                .setDescription("Cette commande demande les droits administrateurs, ce que tu n'as pas (*cheh*).")
                .setColor(Color.RED);
    }

}
