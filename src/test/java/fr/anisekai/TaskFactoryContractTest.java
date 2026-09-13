package fr.anisekai;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.discord.tasks.announcement.AnnouncementTask;
import fr.anisekai.discord.tasks.announcement.AnnouncementTaskFactory;
import fr.anisekai.discord.tasks.announcement.AnnouncementTaskInput;
import fr.anisekai.discord.tasks.announcement.AnnouncementTaskOutput;
import fr.anisekai.discord.tasks.broadcast.cancel.BroadcastCancelTask;
import fr.anisekai.discord.tasks.broadcast.cancel.BroadcastCancelTaskFactory;
import fr.anisekai.discord.tasks.broadcast.cancel.BroadcastCancelTaskInput;
import fr.anisekai.discord.tasks.broadcast.schedule.BroadcastScheduleTask;
import fr.anisekai.discord.tasks.broadcast.schedule.BroadcastScheduleTaskFactory;
import fr.anisekai.discord.tasks.broadcast.schedule.BroadcastScheduleTaskInput;
import fr.anisekai.discord.tasks.broadcast.schedule.BroadcastScheduleTaskOutput;
import fr.anisekai.discord.tasks.watchlist.create.WatchlistCreateTask;
import fr.anisekai.discord.tasks.watchlist.create.WatchlistCreateTaskFactory;
import fr.anisekai.discord.tasks.watchlist.create.WatchlistCreateTaskOutput;
import fr.anisekai.discord.tasks.watchlist.update.WatchlistUpdateTask;
import fr.anisekai.discord.tasks.watchlist.update.WatchlistUpdateTaskFactory;
import fr.anisekai.discord.tasks.watchlist.update.WatchlistUpdateTaskInput;
import fr.anisekai.library.tasks.torrent.retention.TorrentRetentionInput;
import fr.anisekai.library.tasks.torrent.retention.TorrentRetentionTask;
import fr.anisekai.library.tasks.torrent.retention.TorrentRetentionTaskFactory;
import fr.anisekai.library.tasks.torrent.sourcing.TorrentSourcingTask;
import fr.anisekai.library.tasks.torrent.sourcing.TorrentSourcingTaskFactory;
import fr.anisekai.library.tasks.torrent.sourcing.TorrentSourcingTaskInput;
import fr.anisekai.library.tasks.torrent.synchronization.TorrentSynchronizationTask;
import fr.anisekai.library.tasks.torrent.synchronization.TorrentSynchronizationTaskFactory;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.server.domain.enums.AnimeList;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TaskFactoryContractTest {

    private final JsonSerializerFactory serializers = mock(JsonSerializerFactory.class);

    @Test
    void usesStableEntityAndSingletonTaskNames() {

        UUID id = UUID.fromString("018f73a4-5b6c-7def-8123-456789abcdef");

        assertEquals(
                "discord:announcement:" + id,
                this.announcementFactory().getTaskName(new AnnouncementTaskInput(id, false))
        );
        assertEquals(
                "broadcast:cancel:" + id,
                this.broadcastCancelFactory().getTaskName(new BroadcastCancelTaskInput(id))
        );
        assertEquals(
                "broadcast:schedule:" + id,
                this.broadcastScheduleFactory().getTaskName(new BroadcastScheduleTaskInput(id, null, null, null, null, null))
        );
        assertEquals(
                "watchlist:update:watching",
                this.watchlistUpdateFactory().getTaskName(new WatchlistUpdateTaskInput(AnimeList.WATCHING))
        );
        assertEquals("watchlist:create", this.watchlistCreateFactory().getTaskName(Nothing.INSTANCE));
        assertEquals("torrent:synchronize", this.torrentSynchronizationFactory().getTaskName(Nothing.INSTANCE));
        assertEquals(
                "torrent:sourcing",
                this.torrentSourcingFactory().getTaskName(new TorrentSourcingTaskInput("https://example.test/feed", (byte) 1))
        );
        assertEquals(
                "torrent:cleanup",
                this.torrentRetentionFactory().getTaskName(new TorrentRetentionInput(Duration.ofDays(1)))
        );
    }

    @Test
    void exposesMatchingSerializers() {

        ObjectSerializer<?> serializer = mock(ObjectSerializer.class);
        doReturn(serializer).when(this.serializers).emptySerializer();
        doReturn(serializer).when(this.serializers).createSerializer(AnnouncementTaskInput.class);
        doReturn(serializer).when(this.serializers).createSerializer(AnnouncementTaskOutput.class);
        doReturn(serializer).when(this.serializers).createSerializer(WatchlistCreateTaskOutput.class);
        doReturn(serializer).when(this.serializers).createSerializer(WatchlistUpdateTaskInput.class);
        doReturn(serializer).when(this.serializers).createSerializer(BroadcastCancelTaskInput.class);
        doReturn(serializer).when(this.serializers).createSerializer(BroadcastScheduleTaskInput.class);
        doReturn(serializer).when(this.serializers).createSerializer(BroadcastScheduleTaskOutput.class);
        doReturn(serializer).when(this.serializers).createSerializer(TorrentSourcingTaskInput.class);
        doReturn(serializer).when(this.serializers).createSerializer(TorrentRetentionInput.class);

        this.announcementFactory().getArgumentsSerializer();
        this.announcementFactory().getResultSerializer();
        this.watchlistCreateFactory().getArgumentsSerializer();
        this.watchlistCreateFactory().getResultSerializer();
        this.watchlistUpdateFactory().getArgumentsSerializer();
        this.watchlistUpdateFactory().getResultSerializer();
        this.broadcastCancelFactory().getArgumentsSerializer();
        this.broadcastCancelFactory().getResultSerializer();
        this.broadcastScheduleFactory().getArgumentsSerializer();
        this.broadcastScheduleFactory().getResultSerializer();
        this.torrentSynchronizationFactory().getArgumentsSerializer();
        this.torrentSynchronizationFactory().getResultSerializer();
        this.torrentSourcingFactory().getArgumentsSerializer();
        this.torrentSourcingFactory().getResultSerializer();
        this.torrentRetentionFactory().getArgumentsSerializer();
        this.torrentRetentionFactory().getResultSerializer();

        verify(this.serializers).createSerializer(AnnouncementTaskInput.class);
        verify(this.serializers).createSerializer(AnnouncementTaskOutput.class);
        verify(this.serializers).createSerializer(WatchlistCreateTaskOutput.class);
        verify(this.serializers).createSerializer(WatchlistUpdateTaskInput.class);
        verify(this.serializers).createSerializer(BroadcastCancelTaskInput.class);
        verify(this.serializers).createSerializer(BroadcastScheduleTaskInput.class);
        verify(this.serializers).createSerializer(BroadcastScheduleTaskOutput.class);
        verify(this.serializers).createSerializer(TorrentSourcingTaskInput.class);
        verify(this.serializers).createSerializer(TorrentRetentionInput.class);
        verify(this.serializers, times(7)).emptySerializer();
    }

    @Test
    void exposesTaskHandlers() throws Exception {

        assertInstanceOf(AnnouncementTask.class, this.announcementFactory().getHandler());
        assertInstanceOf(WatchlistCreateTask.class, this.watchlistCreateFactory().getHandler());
        assertInstanceOf(WatchlistUpdateTask.class, this.watchlistUpdateFactory().getHandler());
        assertInstanceOf(BroadcastCancelTask.class, this.broadcastCancelFactory().getHandler());
        assertInstanceOf(BroadcastScheduleTask.class, this.broadcastScheduleFactory().getHandler());
        assertInstanceOf(TorrentSynchronizationTask.class, this.torrentSynchronizationFactory().getHandler());
        assertInstanceOf(TorrentSourcingTask.class, this.torrentSourcingFactory().getHandler());
        assertInstanceOf(TorrentRetentionTask.class, this.torrentRetentionFactory().getHandler());
        assertSame(
                Nothing.INSTANCE,
                this.torrentRetentionFactory().getHandler().handle(new TorrentRetentionInput(Duration.ZERO))
        );
    }

    private AnnouncementTaskFactory announcementFactory() {

        return new AnnouncementTaskFactory(this.serializers, null, null, null);
    }

    private WatchlistCreateTaskFactory watchlistCreateFactory() {

        return new WatchlistCreateTaskFactory(this.serializers, null, null, null, null);
    }

    private WatchlistUpdateTaskFactory watchlistUpdateFactory() {

        return new WatchlistUpdateTaskFactory(this.serializers, null, null, null, null);
    }

    private BroadcastCancelTaskFactory broadcastCancelFactory() {

        return new BroadcastCancelTaskFactory(this.serializers, null, null);
    }

    private BroadcastScheduleTaskFactory broadcastScheduleFactory() {

        return new BroadcastScheduleTaskFactory(this.serializers, null, null, null);
    }

    private TorrentSynchronizationTaskFactory torrentSynchronizationFactory() {

        return new TorrentSynchronizationTaskFactory(this.serializers, null);
    }

    private TorrentSourcingTaskFactory torrentSourcingFactory() {

        return new TorrentSourcingTaskFactory(this.serializers, null, null, null, null);
    }

    private TorrentRetentionTaskFactory torrentRetentionFactory() {

        return new TorrentRetentionTaskFactory(this.serializers, null, null, null);
    }

}
