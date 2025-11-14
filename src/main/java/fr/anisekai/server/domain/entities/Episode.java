package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.IncrementableEntity;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

@Entity
public class Episode extends IncrementableEntity implements ScopedEntity, Comparable<Episode> {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Anime anime;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private boolean ready = false;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "episode")
    private Set<Track> tracks;

    public @NotNull Anime getAnime() {

        return this.anime;
    }

    public void setAnime(@NotNull Anime anime) {

        this.anime = anime;
    }

    public int getNumber() {

        return this.number;
    }

    public void setNumber(int number) {

        this.number = number;
    }

    public boolean isReady() {

        return this.ready;
    }

    public void setReady(boolean ready) {

        this.ready = ready;
    }

    public Set<Track> getTracks() {

        return this.tracks;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Episode episode) return EntityUtils.equals(this, episode);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

    @Override
    public @NotNull String getScopedName() {

        if (this.isNew()) throw new IllegalStateException("Cannot use a non persisted entity as scoped entity.");
        return String.valueOf(this.getId());
    }

    @Override
    public int compareTo(@NotNull Episode o) {

        return EntityUtils.compare(
                this,
                o,
                Comparator.comparing(Episode::getAnime),
                Comparator.comparing(Episode::getNumber)
        );
    }

}
