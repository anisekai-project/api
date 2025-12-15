package fr.anisekai.discord.responses;

import fr.anisekai.discord.interfaces.MessageRequestResponse;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.awt.*;
import java.util.function.Consumer;

public class DiscordResponse implements MessageRequestResponse {

    private final EmbedBuilder builder;
    private final boolean      edit;
    private final boolean      ephemeral;

    public DiscordResponse(CharSequence message) {

        this(message, false, false);
    }

    public DiscordResponse(CharSequence message, boolean edit, boolean ephemeral) {

        this(new EmbedBuilder().setDescription(message), edit, ephemeral);
    }

    public DiscordResponse(EmbedBuilder builder, boolean edit, boolean ephemeral) {

        this.builder   = builder;
        this.edit      = edit;
        this.ephemeral = ephemeral;
    }

    public static DiscordResponse of(CharSequence message, Color color) {

        return new DiscordResponse(new EmbedBuilder().setDescription(message).setColor(color), false, false);
    }

    public static DiscordResponse ofPrivate(CharSequence message, Color color) {

        return new DiscordResponse(new EmbedBuilder().setDescription(message).setColor(color), false, true);
    }

    public static DiscordResponse error(String message, Object... args) {

        return of(String.format(message, args), Color.RED);
    }

    public static DiscordResponse warn(String message, Object... args) {

        return of(String.format(message, args), Color.ORANGE);
    }

    public static DiscordResponse info(String message, Object... args) {

        return of(String.format(message, args), Color.CYAN);
    }

    public static DiscordResponse success(String message, Object... args) {

        return of(String.format(message, args), Color.GREEN);
    }

    public static DiscordResponse privateError(String message, Object... args) {

        return ofPrivate(String.format(message, args), Color.RED);
    }

    public static DiscordResponse privateWarn(String message, Object... args) {

        return ofPrivate(String.format(message, args), Color.ORANGE);
    }

    public static DiscordResponse privateInfo(String message, Object... args) {

        return ofPrivate(String.format(message, args), Color.CYAN);
    }

    public static DiscordResponse privateSuccess(String message, Object... args) {

        return ofPrivate(String.format(message, args), Color.GREEN);
    }

    @Override
    public Consumer<MessageRequest<?>> getHandler() {

        return (amb) -> amb.setEmbeds(this.builder.build());
    }

    public DiscordResponse setImage(String url) {

        this.builder.setImage(url);
        return this;
    }

}
