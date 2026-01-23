package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.IncrementableEntity;
import fr.anisekai.server.domain.enums.AnimeSeason;
import fr.anisekai.server.domain.enums.SelectionStatus;
import fr.anisekai.server.domain.events.selection.SelectionAnimesUpdatedEvent;
import fr.anisekai.server.domain.events.selection.SelectionStatusUpdatedEvent;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "selection")
public class Selection extends IncrementableEntity {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnimeSeason season;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @TriggerEvent(SelectionStatusUpdatedEvent.class)
    private SelectionStatus status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "selection_animes",
            joinColumns = @JoinColumn(name = "selection_id"),
            inverseJoinColumns = @JoinColumn(name = "animes_id")
    )
    @TriggerEvent(SelectionAnimesUpdatedEvent.class)
    private Set<Anime> animes;

    public @NotNull AnimeSeason getSeason() {

        return this.season;
    }

    public void setSeason(@NotNull AnimeSeason season) {

        this.season = season;
    }

    public int getYear() {

        return this.year;
    }

    public void setYear(int year) {

        this.year = year;
    }

    public @NotNull SelectionStatus getStatus() {

        return this.status;
    }

    public void setStatus(@NotNull SelectionStatus status) {

        this.status = status;
    }

    public @NotNull Set<Anime> getAnimes() {

        return this.animes;
    }

    public void setAnimes(@NotNull Set<Anime> animes) {

        this.animes = animes;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Selection selection) return EntityUtils.equals(this, selection);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
