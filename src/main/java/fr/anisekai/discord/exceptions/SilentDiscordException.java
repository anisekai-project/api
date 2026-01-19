package fr.anisekai.discord.exceptions;

import fr.anisekai.AnisekaiApplication;
import fr.anisekai.discord.interfaces.DiscordEmbeddable;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.ZonedDateTime;

public abstract class SilentDiscordException extends RuntimeException implements DiscordEmbeddable {

    public SilentDiscordException(String message, Throwable cause) {

        super(message, cause);
    }

    public SilentDiscordException(String message) {

        super(message);
    }

    @NotNull
    public abstract String getFriendlyMessage();

    private String getDiscordMessage() {

        if (AnisekaiApplication.enableDetailedOutput) {
            return "%s\n\n> %s".formatted(this.getFriendlyMessage(), this.getMessage());
        }
        return this.getFriendlyMessage();
    }

    @Override
    public EmbedBuilder asEmbed() {

        return new EmbedBuilder()
                .setAuthor("Anisekai", "https://anisekai.fr", "https://anisekai.fr/favicon.png")
                .setDescription(this.getDiscordMessage())
                .setColor(Color.RED)
                .setTimestamp(ZonedDateTime.now());
    }

}
