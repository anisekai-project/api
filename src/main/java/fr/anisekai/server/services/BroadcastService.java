package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.scheduler.commons.ActionPlan;
import fr.anisekai.scheduler.commons.actions.CreateAction;
import fr.anisekai.scheduler.commons.actions.DeleteAction;
import fr.anisekai.scheduler.commons.actions.UpdateAction;
import fr.anisekai.scheduler.event.EventScheduler;
import fr.anisekai.scheduler.event.data.ReservedSpot;
import fr.anisekai.scheduler.event.exceptions.NotSchedulableException;
import fr.anisekai.scheduler.event.interfaces.ScheduleSpotData;
import fr.anisekai.scheduler.event.interfaces.Scheduler;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.enums.BroadcastFrequency;
import fr.anisekai.server.planifier.BookedSpot;
import fr.anisekai.server.planifier.CalibrationResult;
import fr.anisekai.server.repositories.BroadcastRepository;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class BroadcastService extends AnisekaiService<Broadcast, UUID, BroadcastRepository> {

    public final static List<BroadcastStatus> ACTIVE_STATUSES = Arrays.asList(
            BroadcastStatus.SCHEDULED,
            BroadcastStatus.ACTIVE,
            BroadcastStatus.UNSCHEDULED
    );

    private final DatabaseLockService databaseLockService;

    public BroadcastService(BroadcastRepository repository, EntityEventProcessor eventProcessor, DatabaseLockService databaseLockService) {

        super(repository, eventProcessor);
        this.databaseLockService = databaseLockService;
    }

    private static @NotNull Broadcast createBroadcast(@NotNull ReservedSpot<Anime> spot) {

        Broadcast broadcast = new Broadcast();
        broadcast.setWatchTarget(spot.watchTarget());
        broadcast.setStartingAt(spot.startingAt());
        broadcast.setEpisodeCount(spot.episodeCount());
        broadcast.setFirstEpisode(spot.firstEpisode());
        broadcast.setSkipEnabled(spot.skipEnabled());
        broadcast.setStatus(BroadcastStatus.UNSCHEDULED); // Default list
        return broadcast;
    }

    public boolean hasPreviousScheduled(ScheduleSpotData<Anime> broadcast) {

        return this.getRepository()
                   .countPreviousOf(
                           broadcast.getWatchTarget().getId(),
                           broadcast.getStartingAt(),
                           ACTIVE_STATUSES
                   ) > 0;
    }

    private Scheduler<Anime, Broadcast, UUID> createScheduler() {

        List<Broadcast> items = this.getRepository().findAllByStatusIn(ACTIVE_STATUSES);

        return new EventScheduler<>(items, Broadcast::getId);
    }

    @Transactional
    public List<Broadcast> schedule(Anime anime, Instant starting, BroadcastFrequency frequency, int amount) {

        this.databaseLockService.lock(DatabaseLockService.BROADCAST_SCHEDULE);
        int total = Math.abs(anime.getTotal());

        if (total == 0) {
            throw new IllegalArgumentException("Unknown amount of episodes");
        }

        if (frequency.hasDateModifier()) {
            List<Broadcast> scheduledBroadcasts = new ArrayList<>();
            int             schedulable         = total - anime.getWatched();
            Instant         spotTime            = starting;

            while (schedulable > 0) {
                // Recreate scheduler with current state for accurate conflict detection
                Scheduler<Anime, Broadcast, UUID> loopScheduler = this.createScheduler();
                int                               spotAmount    = Math.min(schedulable, amount);
                ScheduleSpotData<Anime> spot = new BookedSpot<>(
                        anime,
                        spotTime,
                        spotAmount
                );

                if (!loopScheduler.canSchedule(spot)) {
                    throw new NotSchedulableException();
                }

                ActionPlan<UUID, ReservedSpot<Anime>, Broadcast> plan   = loopScheduler.schedule(spot);
                List<Broadcast>                                  result = this.applyPlan(plan);
                scheduledBroadcasts.addAll(result);

                spotTime = frequency.getDateModifier().apply(spotTime);
                schedulable -= spotAmount;
            }
            return scheduledBroadcasts;
        } else {
            Scheduler<Anime, Broadcast, UUID>                scheduler = this.createScheduler();
            ScheduleSpotData<Anime>                          spot      = new BookedSpot<>(anime, starting, amount);
            ActionPlan<UUID, ReservedSpot<Anime>, Broadcast> plan      = scheduler.schedule(spot);
            return this.applyPlan(plan);
        }
    }

    @Transactional
    public List<Broadcast> delay(Instant from, Duration interval, Duration delay) {

        this.databaseLockService.lock(DatabaseLockService.BROADCAST_SCHEDULE);
        Scheduler<Anime, Broadcast, UUID>                scheduler = this.createScheduler();
        ActionPlan<UUID, ReservedSpot<Anime>, Broadcast> plan      = scheduler.delay(from, interval, delay);
        return this.applyPlan(plan);
    }

    @Transactional
    public CalibrationResult calibrate() {

        this.databaseLockService.lock(DatabaseLockService.BROADCAST_SCHEDULE);
        Scheduler<Anime, Broadcast, UUID>                scheduler = this.createScheduler();
        ActionPlan<UUID, ReservedSpot<Anime>, Broadcast> plan      = scheduler.calibrate();

        this.applyPlan(plan);

        return new CalibrationResult(plan.updates().size(), plan.deletes().size());
    }

    public Optional<Broadcast> find(ScheduledEvent event) {

        return this.getRepository().findByEventId(event.getIdLong());
    }

    private List<Broadcast> applyPlan(ActionPlan<UUID, ReservedSpot<Anime>, Broadcast> plan) {

        List<Broadcast> results = new ArrayList<>();

        for (CreateAction<ReservedSpot<Anime>> create : plan.creates()) {
            Broadcast broadcast = createBroadcast(create.what());
            results.add(this.getRepository().save(broadcast));
        }

        for (UpdateAction<UUID, Broadcast> update : plan.updates()) {
            Broadcast target = this.requireById(update.targetId());
            update.hook().accept(target);
            results.add(this.getRepository().save(target));
        }

        for (DeleteAction<UUID> delete : plan.deletes()) {
            this.getRepository().deleteById(delete.targetId());
        }

        return results;
    }

}
