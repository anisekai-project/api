package fr.anisekai.discord.interfaces;

import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.function.Consumer;

public interface InteractionResponse {

    /**
     * Retrieve the {@link MessageRequest} {@link Consumer} that should set the response content.
     *
     * @return A {@link MessageRequest} {@link Consumer}
     */
    Consumer<MessageRequest<?>> getHandler();

}
