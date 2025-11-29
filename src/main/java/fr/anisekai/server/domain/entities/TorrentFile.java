package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.BaseEntity;
import fr.anisekai.server.domain.keys.TorrentKey;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Entity
@IdClass(TorrentKey.class)
public class TorrentFile extends BaseEntity<TorrentKey> {

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    private Torrent torrent;

    @Id
    @Column(nullable = false)
    private int index;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Episode episode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean removed = false;

    @Override
    public TorrentKey getId() {

        return TorrentKey.create(this.getTorrent(), this.index);
    }

    public @NotNull Torrent getTorrent() {

        return this.torrent;
    }

    public void setTorrent(@NotNull Torrent torrent) {

        this.torrent = torrent;
    }

    public int getIndex() {

        return this.index;
    }

    public void setIndex(int index) {

        this.index = index;
    }

    public Episode getEpisode() {

        return this.episode;
    }

    public void setEpisode(Episode episode) {

        this.episode = episode;
    }

    public @NotNull String getName() {

        return this.name;
    }

    public void setName(@NotNull String name) {

        this.name = name;
    }

    public boolean isRemoved() {

        return this.removed;
    }

    public void setRemoved(boolean removed) {

        this.removed = removed;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof TorrentFile torrentFile) return EntityUtils.equals(this, torrentFile);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
