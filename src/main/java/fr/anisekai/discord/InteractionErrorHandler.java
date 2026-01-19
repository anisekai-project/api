package fr.anisekai.discord;

import fr.alexpado.interactions.interfaces.handlers.ErrorHandler;
import fr.alexpado.interactions.interfaces.routing.Request;
import fr.anisekai.discord.interfaces.DiscordEmbeddable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class InteractionErrorHandler implements ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractionErrorHandler.class);

    @Override
    public void handle(@NonNull Throwable throwable, @NonNull Interaction interaction, @Nullable Request<?> request) {

        MessageEmbed embed = this.getEmbedFor(throwable);

        if (request != null) {
            InteractionHook attachment = request.getAttachment(InteractionHook.class);

            if (attachment != null) {
                attachment.editOriginalEmbeds(embed).complete();
                return;
            }
        }

        if (interaction instanceof IReplyCallback reply) {
            if (interaction.isAcknowledged()) {
                reply.getHook().editOriginalEmbeds(embed).complete();
            } else {
                reply.replyEmbeds(embed).setEphemeral(true).complete();
            }
            return;
        }

        // Could not handle this, rethrow.
        throw new IllegalStateException("Could not handle exception", throwable);
    }

    private MessageEmbed getEmbedFor(Throwable throwable) {

        if (throwable instanceof DiscordEmbeddable embeddable) {
            return embeddable.asEmbed().build();
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Une erreur est survenue.");
        builder.setDescription("Une erreur est survenue lors du traitement. Veuillez réessayer plus tard.");
        builder.setColor(Color.RED);

        LOGGER.error("An unmanaged error occurred", throwable);

        return builder.build();
    }

}
