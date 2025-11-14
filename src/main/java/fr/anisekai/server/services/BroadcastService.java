package fr.anisekai.server.services;

import fr.anisekai.core.internal.plannifier.EventScheduler;
import fr.anisekai.core.internal.plannifier.data.CalibrationResult;
import fr.anisekai.core.internal.plannifier.exceptions.NotSchedulableException;
import fr.anisekai.core.internal.plannifier.interfaces.ScheduleSpotData;
import fr.anisekai.core.internal.plannifier.interfaces.Scheduler;
import fr.anisekai.core.internal.plannifier.interfaces.entities.Planifiable;
import fr.anisekai.core.internal.plannifier.plan.SchedulingAction;
import fr.anisekai.core.internal.plannifier.plan.SchedulingActionType;
import fr.anisekai.core.internal.plannifier.plan.SchedulingPlan;
import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.discord.tasks.broadcast.cancel.BroadcastCancelFactory;
import fr.anisekai.discord.tasks.broadcast.schedule.BroadcastScheduleFactory;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.enums.BroadcastFrequency;
import fr.anisekai.server.planifier.BookedSpot;
import fr.anisekai.server.repositories.BroadcastRepository;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class BroadcastService extends AnisekaiService<Broadcast, Long, BroadcastRepository> {

    public final static List<BroadcastStatus> ACTIVE_STATUSES = Arrays.asList(
            BroadcastStatus.SCHEDULED,
            BroadcastStatus.ACTIVE,
            BroadcastStatus.UNSCHEDULED
    );

    private final ApplicationEventPublisher publisher;
    private final TaskService               taskService;

    public BroadcastService(BroadcastRepository repository, EntityEventProcessor eventProcessor, ApplicationEventPublisher publisher, TaskService taskService) {

        super(repository, eventProcessor);
        this.publisher   = publisher;
        this.taskService = taskService;
    }

    public boolean hasPreviousScheduled(ScheduleSpotData<Anime> broadcast) {

        return this.getRepository()
                   .countPreviousOf(
                           broadcast.getWatchTarget().getId(),
                           broadcast.getStartingAt(),
                           ACTIVE_STATUSES
                   ) > 0;
    }

    private Scheduler<Anime, Broadcast, Long> createScheduler() {

        List<Broadcast> items = this.getRepository().findAllByStatusIn(ACTIVE_STATUSES);

        return new EventScheduler<>(items, Broadcast::getId);
    }

    @Transactional
    public List<Broadcast> schedule(Anime anime, ZonedDateTime starting, BroadcastFrequency frequency, int amount) {

        int total = Math.abs(anime.getTotal());

        if (total == 0) {
            throw new IllegalArgumentException("Unknown amount of episodes");
        }

        if (frequency.hasDateModifier()) {
            List<Broadcast> scheduledBroadcasts = new ArrayList<>();
            int             schedulable         = total - anime.getWatched();
            ZonedDateTime   spotTime            = starting;

            while (schedulable > 0) {
                // Recreate scheduler with current state for accurate conflict detection
                Scheduler<Anime, Broadcast, Long> loopScheduler = this.createScheduler();
                int                               spotAmount    = Math.min(schedulable, amount);
                BookedSpot<Anime> spot = new BookedSpot<>(
                        anime,
                        spotTime,
                        spotAmount
                );

                if (!loopScheduler.canSchedule(spot)) {
                    throw new NotSchedulableException();
                }

                SchedulingPlan<Long> plan   = loopScheduler.schedule(spot);
                List<Broadcast>      result = this.applyPlan(plan);
                scheduledBroadcasts.addAll(result);

                spotTime = frequency.getDateModifier().apply(spotTime);
                schedulable -= spotAmount;
            }
            return scheduledBroadcasts;
        } else {
            Scheduler<Anime, Broadcast, Long> scheduler = this.createScheduler();
            BookedSpot<Anime>                 spot      = new BookedSpot<>(anime, starting, amount);
            SchedulingPlan<Long>              plan      = scheduler.schedule(spot);
            return this.applyPlan(plan);
        }
    }

    @Transactional
    public List<Broadcast> delay(ZonedDateTime from, Duration interval, Duration delay) {

        Scheduler<Anime, Broadcast, Long> scheduler = this.createScheduler();
        SchedulingPlan<Long>              plan      = scheduler.delay(from, interval, delay);
        return this.applyPlan(plan);
    }

    @Transactional
    public CalibrationResult calibrate() {

        Scheduler<Anime, Broadcast, Long> scheduler = this.createScheduler();
        SchedulingPlan<Long>              plan      = scheduler.calibrate();

        this.applyPlan(plan);

        long updateCount = plan.getActions()
                               .stream()
                               .filter(a -> a.getActionType() == SchedulingActionType.UPDATE)
                               .count();
        long deleteCount = plan.getActions()
                               .stream()
                               .filter(a -> a.getActionType() == SchedulingActionType.DELETE)
                               .count();

        return new CalibrationResult((int) updateCount, (int) deleteCount);
    }

    public int refresh() {

        List<Broadcast> broadcasts = this.getRepository().findAllByStatus(BroadcastStatus.SCHEDULED);

        for (Broadcast broadcast : broadcasts) {
            this.taskService.getFactory(BroadcastScheduleFactory.class).queue(broadcast);
        }
        return broadcasts.size();
    }

    @Transactional
    public int cancel() {

        List<Broadcast> broadcasts = this.getRepository().findAllByStatus(BroadcastStatus.ACTIVE);

        for (Broadcast broadcast : broadcasts) {
            String taskName = this.taskService.getFactory(BroadcastCancelFactory.class).asTaskName(broadcast);

            if (this.taskService.has(taskName)) {
                continue;
            }

            String scheduleTaskName = this.taskService.getFactory(BroadcastScheduleFactory.class).asTaskName(broadcast);
            this.taskService.cancel(scheduleTaskName);

            if (broadcast.getStatus().isDiscordCancelable()) {
                this.taskService.getFactory(BroadcastCancelFactory.class).queue(broadcast);
            } else {
                broadcast.setStatus(BroadcastStatus.CANCELED);
                this.getRepository().save(broadcast);
            }
        }

        return broadcasts.size();
    }

    @Transactional
    public boolean cancel(ScheduledEvent event) {

        Optional<Broadcast> optionalBroadcast = this.getRepository().findByEventId(event.getIdLong());

        if (optionalBroadcast.isEmpty()) {
            return false;
        }

        Broadcast              broadcast = optionalBroadcast.get();
        BroadcastCancelFactory factory   = this.taskService.getFactory(BroadcastCancelFactory.class);
        String                 name      = factory.asTaskName(broadcast);

        if (this.taskService.has(name)) {
            this.taskService.cancel(name);
        }

        broadcast.setStatus(BroadcastStatus.CANCELED);
        this.getRepository().save(broadcast);
        return true;
    }

    public Broadcast cancel(Broadcast broadcast) {

        BroadcastCancelFactory factory = this.taskService.getFactory(BroadcastCancelFactory.class);
        String                 name    = factory.asTaskName(broadcast);

        if (this.taskService.has(name)) {
            this.taskService.cancel(name);
        }

        broadcast.setStatus(BroadcastStatus.CANCELED);
        return this.getRepository().save(broadcast);
    }

    public Optional<Broadcast> find(ScheduledEvent event) {

        return this.getRepository().findByEventId(event.getIdLong());
    }


    private List<Broadcast> applyPlan(SchedulingPlan<Long> plan) {

        List<Broadcast> results = new ArrayList<>();
        for (SchedulingAction<Long> action : plan.getActions()) {
            switch (action) {
                case SchedulingAction.CreateAction<Long> createAction -> {
                    if (createAction.data() instanceof Planifiable<?> planifiable && planifiable.getWatchTarget() instanceof Anime anime) {
                        Broadcast newBroadcast = createBroadcast(planifiable, anime);
                        results.add(this.getRepository().save(newBroadcast));
                    }
                }
                case SchedulingAction.UpdateAction<Long> updateAction -> {
                    Broadcast target = this.requireById(updateAction.targetId());
                    updateAction.updateHook().accept(target);
                    results.add(this.getRepository().save(target));
                }
                case SchedulingAction.DeleteAction<Long> deleteAction -> {
                    this.getRepository().deleteById(deleteAction.targetId());
                }
            }
        }
        return results;
    }

    private static @NotNull Broadcast createBroadcast(@NotNull Planifiable<?> planifiable, @NotNull Anime anime) {

        Broadcast broadcast = new Broadcast();
        broadcast.setWatchTarget(anime);
        broadcast.setStartingAt(planifiable.getStartingAt());
        broadcast.setEpisodeCount(planifiable.getEpisodeCount());
        broadcast.setFirstEpisode(planifiable.getFirstEpisode());
        broadcast.setSkipEnabled(planifiable.isSkipEnabled());
        broadcast.setStatus(BroadcastStatus.UNSCHEDULED); // Default status
        return broadcast;
    }

}
