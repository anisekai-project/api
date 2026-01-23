package fr.anisekai.discord.interfaces;

public interface ButtonResponse extends InteractionResponse {

    /**
     * Check if the {@link ButtonResponse} should edit the original message.
     *
     * @return {@code true} if the original should be edited, {@code false} otherwise.
     */
    default boolean mayEditMessage() {

        return true;
    }

}
