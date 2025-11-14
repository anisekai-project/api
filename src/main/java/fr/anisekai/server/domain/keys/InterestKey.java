package fr.anisekai.server.domain.keys;

import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Represents a composite key linking a specific anime to a specific user.
 * <p>
 * Used to uniquely identify a user's interest in a particular anime.
 *
 * @param anime
 *         The {@link Anime} ID
 * @param user
 *         The {@link DiscordUser} ID
 */
public record InterestKey(long anime, long user) implements Serializable {

    /**
     * Creates a new {@link InterestKey} from the given {@link Anime} and {@link DiscordUser}.
     * <p>
     * Both entities must have non-null IDs.
     *
     * @param anime
     *         The {@link Anime}
     * @param user
     *         The {@link DiscordUser}
     *
     * @return A new {@link InterestKey} representing the link between an {@link Anime} and an {@link DiscordUser}.
     *
     * @throws AssertionError
     *         Threw if either {@link Anime#getId()} or {@link DiscordUser#getId()} is {@code null}
     */
    public static @NotNull InterestKey create(@NotNull Anime anime, @NotNull DiscordUser user) {

        assert anime.getId() != null;
        assert user.getId() != null;
        return new InterestKey(anime.getId(), user.getId());
    }

}
