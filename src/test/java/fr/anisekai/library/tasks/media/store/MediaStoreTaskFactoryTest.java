package fr.anisekai.library.tasks.media.store;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.EpisodeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaStoreTaskFactoryTest {

    @Test
    void usesEpisodeSpecificTaskName() {

        UUID episodeId = UUID.randomUUID();
        MediaStoreTaskFactory factory = new MediaStoreTaskFactory(null, null);

        assertEquals("media:store:" + episodeId, factory.getTaskName(new MediaStoreTaskInput(episodeId, true)));
    }

    @Test
    void successfulPacketMarksEpisodeReady() {

        UUID episodeId = UUID.randomUUID();
        MediaStoreTaskInput input = new MediaStoreTaskInput(episodeId, true);
        @SuppressWarnings("unchecked")
        ObjectSerializer<MediaStoreTaskInput> serializer = mock(ObjectSerializer.class);
        JsonSerializerFactory serializerFactory = mock(JsonSerializerFactory.class);
        EpisodeService episodeService = mock(EpisodeService.class);
        Task task = new Task();
        task.setArguments("input");

        when(serializerFactory.createSerializer(MediaStoreTaskInput.class)).thenReturn(serializer);
        when(serializer.deserialize("input")).thenReturn(input);

        MediaStoreTaskFactory factory = new MediaStoreTaskFactory(serializerFactory, episodeService);
        factory.onSuccess(new TaskExecutedPacket<>(task, Nothing.INSTANCE));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Episode>> readinessCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(episodeService).mod(eq(episodeId), readinessCaptor.capture());

        Episode episode = new Episode();
        readinessCaptor.getValue().accept(episode);

        assertTrue(episode.isReady());
    }

}
