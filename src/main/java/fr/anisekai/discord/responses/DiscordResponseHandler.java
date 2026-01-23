package fr.anisekai.discord.responses;

import fr.alexpado.interactions.interfaces.handlers.ResponseHandler;
import fr.alexpado.interactions.interfaces.routing.Request;
import fr.anisekai.discord.interfaces.ButtonResponse;
import fr.anisekai.discord.interfaces.InteractionResponse;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jspecify.annotations.NonNull;

public class DiscordResponseHandler implements ResponseHandler<InteractionResponse> {

    @Override
    public void handle(@NonNull Request<? extends IReplyCallback> request, @NonNull InteractionResponse response) {

        IReplyCallback interaction = request.getEvent();

        boolean acknowledged = interaction.isAcknowledged();
        boolean editButton   = response instanceof ButtonResponse button && button.mayEditMessage();

        if (interaction instanceof IMessageEditCallback editor && editButton) {
            MessageEditBuilder builder = new MessageEditBuilder();
            response.getHandler().accept(builder);
            editor.editMessage(builder.build()).complete();
            return;
        }

        if (acknowledged || editButton) {
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
