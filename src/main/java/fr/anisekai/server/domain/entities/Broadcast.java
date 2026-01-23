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

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "broadcast")
public class Broadcast extends IncrementableEntity implements Planifiable<Anime> {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "watch_target_id")
    private Anime watchTarget;

    @Column(name = "starting_at", nullable = false)
    @TriggerEvent(BroadcastStartingAtUpdatedEvent.class)
    private Instant startingAt;

    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @TriggerEvent(BroadcastStatusUpdatedEvent.class)
    private BroadcastStatus status;


    @Column(name = "episode_count", nullable = false)
    @TriggerEvent(BroadcastEpisodeCountUpdatedEvent.class)
    private int episodeCount;

    @Column(name = "first_episode", nullable = false)
    @TriggerEvent(BroadcastFirstEpisodeUpdatedEvent.class)
    private int firstEpisode;

    @Column(name = "skip_enabled", nullable = false)
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
    public @NotNull Instant getStartingAt() {

        return this.startingAt;
    }

    @Override
    public void setStartingAt(@NotNull Instant startingAt) {

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
