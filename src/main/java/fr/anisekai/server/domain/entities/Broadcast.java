package fr.anisekai.server.domain.entities;

import fr.anisekai.core.internal.plannifier.interfaces.entities.Planifiable;
import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.IncrementableEntity;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.domain.events.broadcast.BroadcastEpisodeCountUpdatedEvent;
import fr.anisekai.server.domain.events.broadcast.BroadcastFirstEpisodeUpdatedEvent;
import fr.anisekai.server.domain.events.broadcast.BroadcastStartingAtUpdatedEvent;
import fr.anisekai.server.domain.events.broadcast.BroadcastStatusUpdatedEvent;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
public class Broadcast extends IncrementableEntity implements Planifiable<Anime> {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Anime watchTarget;

    @Column(nullable = false)
    @TriggerEvent(BroadcastStartingAtUpdatedEvent.class)
    private ZonedDateTime startingAt;

    @Column
    private Long eventId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @TriggerEvent(BroadcastStatusUpdatedEvent.class)
    private BroadcastStatus status;

    @Column(nullable = false)
    @TriggerEvent(BroadcastEpisodeCountUpdatedEvent.class)
    private int episodeCount;

    @Column(nullable = false)
    @TriggerEvent(BroadcastFirstEpisodeUpdatedEvent.class)
    private int firstEpisode;

    @Column(nullable = false)
    private boolean skipEnabled;

    @Override
    public @NotNull Anime getWatchTarget() {

        return this.watchTarget;
    }

    @Override
    public void setWatchTarget(@NotNull Anime watchTarget) {

        this.watchTarget = watchTarget;
    }

    @Override
    public @NotNull ZonedDateTime getStartingAt() {

        return this.startingAt;
    }

    @Override
    public void setStartingAt(@NotNull ZonedDateTime startingAt) {

        this.startingAt = startingAt;
    }

    public @Nullable Long getEventId() {

        return this.eventId;
    }

    public void setEventId(Long eventId) {

        this.eventId = eventId;
    }

    public @NotNull BroadcastStatus getStatus() {

        return this.status;
    }

    public void setStatus(@NotNull BroadcastStatus status) {

        this.status = status;
    }

    @Override
    public int getEpisodeCount() {

        return this.episodeCount;
    }

    @Override
    public void setEpisodeCount(int episodeCount) {

        this.episodeCount = episodeCount;
    }

    @Override
    public int getFirstEpisode() {

        return this.firstEpisode;
    }

    @Override
    public void setFirstEpisode(int firstEpisode) {

        this.firstEpisode = firstEpisode;
    }

    @Override
    public boolean isSkipEnabled() {

        return this.skipEnabled;
    }

    @Override
    public void setSkipEnabled(boolean skipEnabled) {

        this.skipEnabled = skipEnabled;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Broadcast broadcast) return EntityUtils.equals(this, broadcast);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
