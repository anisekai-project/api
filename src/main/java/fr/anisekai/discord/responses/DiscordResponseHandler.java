package fr.anisekai.discord.responses;

import fr.alexpado.interactions.interfaces.handlers.ResponseHandler;
import fr.alexpado.interactions.interfaces.routing.Request;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jspecify.annotations.NonNull;

public class DiscordResponseHandler implements ResponseHandler<DiscordResponse> {

    @Override
    public void handle(@NonNull Request<? extends IReplyCallback> request, @NonNull DiscordResponse response) {

        IReplyCallback interaction = request.getEvent();

        if (interaction.isAcknowledged()) {
            MessageEditBuilder builder = new MessageEditBuilder();
            response.getHandler().accept(builder);

            interaction.getHook().editOriginal(builder.build()).complete();
        } else {
            MessageCreateBuilder builder = new MessageCreateBuilder();
            response.getHandler().accept(builder);

            interaction.reply(builder.build()).complete();
        }
    }

}
