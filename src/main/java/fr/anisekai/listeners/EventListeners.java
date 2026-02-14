package fr.anisekai.listeners;

import fr.anisekai.core.persistence.EventContextRegistry;
import fr.anisekai.core.persistence.events.EntityCreatedEvent;
import fr.anisekai.core.persistence.events.EntityDeletedEvent;
import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.discord.tasks.anime.announcement.create.AnnouncementCreateFactory;
import fr.anisekai.discord.tasks.anime.announcement.update.AnnouncementUpdateFactory;
import fr.anisekai.discord.tasks.broadcast.schedule.BroadcastScheduleFactory;
import fr.anisekai.discord.tasks.watchlist.update.WatchlistUpdateFactory;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.entities.Voter;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.domain.events.anime.*;
import fr.anisekai.server.domain.events.broadcast.BroadcastEpisodeCountUpdatedEvent;
import fr.anisekai.server.domain.events.broadcast.BroadcastFirstEpisodeUpdatedEvent;
import fr.anisekai.server.domain.events.broadcast.BroadcastStartingAtUpdatedEvent;
import fr.anisekai.server.domain.events.broadcast.BroadcastStatusUpdatedEvent;
import fr.anisekai.server.domain.events.interest.InterestLevelUpdatedEvent;
import fr.anisekai.server.domain.events.selection.SelectionStatusUpdatedEvent;
import fr.anisekai.server.domain.events.user.UserEmoteUpdatedEvent;
import fr.anisekai.server.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class responsible to listen for events from one entity that should impact another entity or impact discord.
 */
@Component
public class EventListeners {

    private final static Logger LOGGER = LoggerFactory.getLogger(EventListeners.class);

    private final EventContextRegistry registry;
    private final SettingService       settingService;
    private final TaskService          taskService;
    private final AnimeService         animeService;
    private final VoterService         voterService;
    private final BroadcastService     broadcastService;

    public EventListeners(EventContextRegistry registry, SettingService settingService, TaskService taskService, AnimeService animeService, VoterService voterService, BroadcastService broadcastService) {

        this.registry = registry;
        this.settingService   = settingService;
        this.taskService      = taskService;
        this.animeService     = animeService;
        this.voterService     = voterService;
        this.broadcastService = broadcastService;
    }

    @EventListener
    public void onEntityCreated(EntityCreatedEvent<?> event) {

        LOGGER.debug("Received created event {}", event.getClass().getSimpleName());
    }

    @EventListener
    public void onEntityDeleted(EntityDeletedEvent<?> event) {

        LOGGER.debug("Received deleted event {}", event.getClass().getSimpleName());
    }

    @EventListener
    public void onEntityUpdated(EntityUpdatedEvent<?, ?> event) {

        LOGGER.debug("Received updated event {}", event.getClass().getSimpleName());
    }

    // <editor-fold desc="Anime">

    @EventListener
    public void onAnimeCreated(EntityCreatedEvent<Anime> event) {

        this.registry.withEventContext(() -> {
            if (this.settingService.isAnimeAnnouncementEnabled()) {
                this.taskService.getFactory(AnnouncementCreateFactory.class).queue(event.getEntity());
            }

            if (event.getEntity().getList().hasProperty(AnimeList.Property.SHOW)) {
                this.taskService.getFactory(WatchlistUpdateFactory.class).queue(event.getEntity().getList());
            }
        });
    }

    @EventListener({
            AnimeUrlUpdatedEvent.class,
            AnimeSynopsisUpdatedEvent.class,
            AnimeTagsUpdatedEvent.class,
            AnimeThumbnailUpdatedEvent.class,
            AnimeTitleUpdatedEvent.class,
            AnimeTotalUpdatedEvent.class,
    })
    public void onAnimeGenericUpdated(EntityUpdatedEvent<Anime, ?> event) {

        this.registry.withEventContext(() -> {
            if (event.getEntity().getAnnouncementId() != null) {
                this.taskService.getFactory(AnnouncementUpdateFactory.class).queue(event.getEntity());
            }
        });
    }

    @EventListener({
            AnimeUrlUpdatedEvent.class,
            AnimeTitleUpdatedEvent.class
    })
    public void onAnimeDataUpdated(EntityUpdatedEvent<Anime, ?> event) {

        this.registry.withEventContext(() -> {
            if (event.getEntity().getList().hasProperty(AnimeList.Property.SHOW)) {
                this.taskService.getFactory(WatchlistUpdateFactory.class).queue(event.getEntity().getList());
            }
        });
    }

    @EventListener
    public void onAnimeStatusUpdated(AnimeListUpdatedEvent event) {

        this.registry.withEventContext(() -> {
            if (event.getPrevious().hasProperty(AnimeList.Property.SHOW)) {
                this.taskService.getFactory(WatchlistUpdateFactory.class).queue(event.getPrevious());
            }

            if (event.getCurrent().hasProperty(AnimeList.Property.SHOW)) {
                this.taskService.getFactory(WatchlistUpdateFactory.class).queue(event.getCurrent());
            }
        });
    }

    @EventListener({AnimeTotalUpdatedEvent.class, AnimeWatchedUpdatedEvent.class})
    public void onAnimeEpisodeValueUpdated(EntityUpdatedEvent<Anime, Integer> event) {

        this.registry.withEventContext(() -> {
            Anime   anime            = event.getEntity();
            boolean hasBeenFinished  = anime.getWatched() == anime.getTotal();
            boolean isTaggedFinished = anime.getList() == AnimeList.WATCHED;

            if (hasBeenFinished && !isTaggedFinished) {
                this.animeService.mod(anime.getId(), entity -> entity.setList(AnimeList.WATCHED));
                return;
            }

            if (anime.getList().hasProperty(AnimeList.Property.PROGRESS)) {
                this.taskService.getFactory(WatchlistUpdateFactory.class).queue(anime.getList());
            }

            if (event instanceof AnimeWatchedUpdatedEvent watchedUpdatedEvent) {
                if (watchedUpdatedEvent.getPrevious() == 0 && watchedUpdatedEvent.getCurrent() > 0) {
                    this.animeService.mod(anime.getId(), this.animeService.defineWatching());
                }
            }
        });
    }

    // </editor-fold>

    // <editor-fold desc="Broadcast">

    @EventListener
    public void onBroadcastCreated(EntityCreatedEvent<Broadcast> event) {

        this.registry.withEventContext(() -> {
            if (!this.broadcastService.hasPreviousScheduled(event.getEntity())) {
                this.taskService.getFactory(BroadcastScheduleFactory.class).queue(event.getEntity());
            } else {
                LOGGER.info("Broadcast {} set to be scheduled later.", event.getEntity().getId());
            }
        });
    }

    @EventListener
    public void onBroadcastStatusUpdate(BroadcastStatusUpdatedEvent event) {

        this.registry.withEventContext(() -> {
            Broadcast broadcast = event.getEntity();
            if (event.getCurrent() == BroadcastStatus.CANCELED) {
                return;
            }

            Anime anime = broadcast.getWatchTarget();

            switch (event.getCurrent()) {
                case ACTIVE -> this.animeService.mod(anime.getId(), this.animeService.defineWatching());
                case COMPLETED -> this.animeService.mod(
                        anime.getId(),
                        this.animeService.defineScheduleProgress(broadcast)
                );
            }

            // Special treatment for unscheduled events that are waiting.
            // Keeping it separated from the logic above
            if (event.getCurrent() == BroadcastStatus.CANCELED || event.getCurrent() == BroadcastStatus.COMPLETED) {
                List<Broadcast> broadcasts = this.broadcastService
                        .getRepository()
                        .findByWatchTargetAndStartingAtAfterOrderByStartingAtAsc(
                                broadcast.getWatchTarget(),
                                broadcast.getStartingAt()
                        );

                if (!broadcasts.isEmpty()) {
                    Broadcast first = broadcasts.getFirst();
                    this.taskService.getFactory(BroadcastScheduleFactory.class).queue(first);
                }
            }
        });
    }

    @EventListener({
            BroadcastFirstEpisodeUpdatedEvent.class,
            BroadcastStartingAtUpdatedEvent.class,
            BroadcastEpisodeCountUpdatedEvent.class,
    })
    public void onBroadcastStateUpdated(EntityUpdatedEvent<Broadcast, ?> event) {

        this.registry.withEventContext(() -> {
            if (event.getEntity().getStatus() == BroadcastStatus.SCHEDULED) {
                this.taskService.getFactory(BroadcastScheduleFactory.class).queue(event.getEntity());
            }
        });
    }

    // </editor-fold>

    // <editor-fold desc="User">

    @EventListener
    public void onUserEmoteUpdated(UserEmoteUpdatedEvent event) {

        this.registry.withEventContext(() -> {
            // TODO: Optimisation possible, only select watchlist where the user has at least a single interest
            for (AnimeList status : AnimeList.collect(AnimeList.Property.SHOW)) {
                this.taskService.getFactory(WatchlistUpdateFactory.class).queue(status);
            }
        });
    }

    // </editor-fold>

    // <editor-fold desc="Interest">

    @EventListener
    public void onInterestCreate(EntityCreatedEvent<Interest> event) {

        this.registry.withEventContext(() -> {
            this.taskService.getFactory(AnnouncementCreateFactory.class).queue(event.getEntity().getAnime());
            this.taskService.getFactory(WatchlistUpdateFactory.class).queue(event.getEntity().getAnime().getList());
        });
    }

    @EventListener
    public void onInterestUpdated(InterestLevelUpdatedEvent event) {

        this.registry.withEventContext(() -> {
            this.taskService.getFactory(AnnouncementCreateFactory.class).queue(event.getEntity().getAnime());
            this.taskService.getFactory(WatchlistUpdateFactory.class).queue(event.getEntity().getAnime().getList());
        });
    }

    // </editor-fold>

    // <editor-fold desc="Selection">

    @EventListener
    public void onSelectionStateUpdated(SelectionStatusUpdatedEvent event) {

        this.registry.withEventContext(() -> {
            List<Long> ids = switch (event.getCurrent()) {
                case OPEN -> Collections.emptyList();
                case CLOSED -> this.voterService
                        .getVoters(event.getEntity())
                        .stream()
                        .map(Voter::getVotes)
                        .flatMap(Set::stream)
                        .map(Anime::getId)
                        .distinct()
                        .collect(Collectors.toList());
                case AUTO_CLOSED -> event.getEntity()
                                         .getAnimes()
                                         .stream()
                                         .map(Anime::getId)
                                         .distinct()
                                         .collect(Collectors.toList());
            };

            this.animeService.move(ids, AnimeList.SIMULCAST);
        });
    }

    // </editor-fold>

}
