package fr.anisekai.server.domain.keys;

import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * A composite key representing a voter's participation in a specific selection, identified by the selection ID and
 * voter ID.
 *
 * @param selection
 *         The ID of the selection
 * @param user
 *         The ID of the voter (user)
 */
public record VoterKey(long selection, long user) implements Serializable {

    /**
     * Creates a new {@link VoterKey} instance from a {@link Selection} and a {@link DiscordUser}.
     *
     * @param selection
     *         The selection in which the user voted
     * @param user
     *         The user who voted
     *
     * @return A new {@link VoterKey} instance
     *
     * @throws AssertionError
     *         Threw if either ID is {@code null}.
     */
    public static @NotNull VoterKey create(@NotNull Selection selection, @NotNull DiscordUser user) {

        assert selection.getId() != null;
        assert user.getId() != null;
        return new VoterKey(selection.getId(), user.getId());
    }

}
