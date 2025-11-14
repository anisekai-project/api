package fr.anisekai.server.domain.keys;

import fr.anisekai.server.domain.entities.Torrent;
import org.hibernate.annotations.JdbcTypeCode;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.sql.Types;
import java.util.UUID;

/**
 * A composite key representing a specific file within a torrent, identified by its torrent ID and file index.
 *
 * @param torrent
 *         The unique identifier of the torrent
 * @param index
 *         The file index within the torrent
 */
public record TorrentKey(@JdbcTypeCode(Types.BINARY) UUID torrent, int index) implements Serializable {

    /**
     * Creates a new {@link TorrentKey} instance from a {@link Torrent} and a file index.
     *
     * @param torrent
     *         the torrent entity
     * @param index
     *         the index of the file inside the torrent
     *
     * @return A new {@link TorrentKey} instance
     *
     * @throws AssertionError
     *         Threw if the torrent ID is {@code null} or the index is negative.
     */
    public static @NotNull TorrentKey create(@NotNull Torrent torrent, int index) {

        assert torrent.getId() != null;
        assert index >= 0;
        return new TorrentKey(torrent.getId(), index);
    }

}

