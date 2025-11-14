package fr.anisekai.server.domain.enums;

import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;

/**
 * Represents the status of a {@link Selection}, indicating whether it is open for voting or has been closed, either
 * manually or automatically.
 */
public enum SelectionStatus {

    /**
     * The {@link Selection} is opened and accepting votes.
     */
    OPEN(false),

    /**
     * The {@link Selection} has been manually closed by an {@link DiscordUser} with application administrator
     * privileges.
     */
    CLOSED(true),

    /**
     * The {@link Selection} has been automatically closed because the number of {@link Anime} entries is less than or
     * equal to the number of votes required, rendering the voting process unnecessary.
     */
    AUTO_CLOSED(true);


    private final boolean closed;

    SelectionStatus(boolean closed) {

        this.closed = closed;
    }

    /**
     * Indicates whether the current status represents a closed state.
     *
     * @return True if the selection is closed, false otherwise
     */
    public boolean isClosed() {

        return this.closed;
    }
}
