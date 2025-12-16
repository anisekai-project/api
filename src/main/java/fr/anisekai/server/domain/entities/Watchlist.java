package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.BaseEntity;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@Entity
public class Watchlist extends BaseEntity<AnimeList> {

    @Id
    @Enumerated(EnumType.STRING)
    private AnimeList id;

    @Column
    private Long messageId;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "list")
    private Set<Anime> animes;

    @Override
    public AnimeList getId() {

        return this.id;
    }

    @Override
    public void setId(AnimeList id) {

        this.id = id;
    }

    public @Nullable Long getMessageId() {

        return this.messageId;
    }

    public void setMessageId(@Nullable Long messageId) {

        this.messageId = messageId;
    }

    public @NotNull Set<Anime> getAnimes() {

        return this.animes;
    }

    public void setAnimes(@NotNull Set<Anime> animes) {

        this.animes = animes;
    }

    public boolean equals(Object o) {

        if (o instanceof Watchlist watchlist) return EntityUtils.equals(this, watchlist);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
