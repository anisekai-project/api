package fr.anisekai.server.domain.enums;

import fr.anisekai.server.domain.entities.Broadcast;

/**
 * Enum representing a {@link Broadcast} schedule state.
 */
public enum BroadcastStatus {

    /**
     * The {@link Broadcast} is only saved in the database and has not been scheduled on Discord.
     */
    UNSCHEDULED(false),

    /**
     * The {@link Broadcast} has been scheduled on Discord.
     */
    SCHEDULED(true),

    /**
     * The {@link Broadcast} is currently active (being broadcasted)
     */
    ACTIVE(true),

    /**
     * The {@link Broadcast} has been broadcasted.
     */
    COMPLETED(false),

    /**
     * The {@link Broadcast} has been canceled.
     */
    CANCELED(false);

    private final boolean discordCancelable;

    BroadcastStatus(boolean discordCancelable) {

        this.discordCancelable = discordCancelable;
    }

    /**
     * Check if tDiscord would accept a cancel query on the current {@link BroadcastStatus}. This is completely
     * different from knowing if a {@link Broadcast} is cancelable or not, as it is completely dependent on the
     * implementation.
     *
     * @return True if it requires a Discord request to be completely canceled, false otherwise.
     */
    public boolean isDiscordCancelable() {

        return this.discordCancelable;
    }
}
