package fr.anisekai.server.planifier;


import fr.anisekai.core.internal.plannifier.interfaces.ScheduleSpotData;
import fr.anisekai.core.internal.plannifier.interfaces.entities.WatchTarget;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

public class BookedSpot<T extends WatchTarget> implements ScheduleSpotData<T> {

    private T             target;
    private ZonedDateTime startingAt;
    private int           episodeCount;

    public BookedSpot(T target, ZonedDateTime startingAt, int episodeCount) {

        this.target       = target;
        this.startingAt   = startingAt;
        this.episodeCount = episodeCount;
    }

    @Override
    public @NotNull T getWatchTarget() {

        return this.target;
    }

    @Override
    public void setWatchTarget(@NotNull T watchTarget) {

        this.target = watchTarget;
    }

    @Override
    public @NotNull ZonedDateTime getStartingAt() {

        return this.startingAt;
    }

    @Override
    public void setStartingAt(@NotNull ZonedDateTime time) {

        this.startingAt = time;
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
    public boolean isSkipEnabled() {

        return true;
    }

    @Override
    public void setSkipEnabled(boolean skipEnabled) {

    }

}
