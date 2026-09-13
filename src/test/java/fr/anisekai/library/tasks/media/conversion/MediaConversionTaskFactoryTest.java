package fr.anisekai.library.tasks.media.conversion;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.library.Library;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.wireless.tasks.conversion.MediaConversionInput;
import fr.anisekai.wireless.tasks.conversion.MediaConversionOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaConversionTaskFactoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsImportsRelativeInputAndSourceSpecificName() throws Exception {

        Path imports = Files.createDirectories(this.temporaryDirectory.resolve("imports/batch"));
        Path source = Files.writeString(imports.resolve("episode.mkv"), "episode");
        Library library = libraryWithImports(this.temporaryDirectory.resolve("imports"));
        Episode episode = episode();

        MediaConversionInput input = MediaConversionTaskFactory.createInput(library, episode, source);
        MediaConversionTaskFactory factory = new MediaConversionTaskFactory(null, null, null, null);

        assertEquals("batch/episode.mkv", input.episode().sourceReference());
        assertTrue(input.episode().hash().matches("[0-9a-f]{64}"));
        assertEquals(
                "media:convert:%s:batch/episode.mkv".formatted(episode.getId()),
                factory.getTaskName(input)
        );
    }

    @Test
    void rejectsSourceOutsideImports() throws Exception {

        Path imports = Files.createDirectory(this.temporaryDirectory.resolve("imports"));
        Path outside = Files.writeString(this.temporaryDirectory.resolve("episode.mkv"), "episode");

        assertThrows(
                IllegalArgumentException.class,
                () -> MediaConversionTaskFactory.createInput(libraryWithImports(imports), episode(), outside)
        );
    }

    @Test
    void successPacketPersistsTracksAndPublishesFollowUpEvent() {

        UUID episodeId = UUID.randomUUID();
        MediaConversionInput input = input(episodeId);
        MediaConversionOutput output = new MediaConversionOutput(List.of());
        @SuppressWarnings("unchecked")
        ObjectSerializer<MediaConversionInput> serializer = mock(ObjectSerializer.class);
        JsonSerializerFactory serializerFactory = mock(JsonSerializerFactory.class);
        EpisodeService episodeService = mock(EpisodeService.class);
        TrackService trackService = mock(TrackService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        Episode episode = episode(episodeId);
        Task task = new Task();
        task.setArguments("input");

        when(serializerFactory.createSerializer(MediaConversionInput.class)).thenReturn(serializer);
        when(serializer.deserialize("input")).thenReturn(input);
        when(episodeService.requireById(episodeId)).thenReturn(episode);

        MediaConversionTaskFactory factory = new MediaConversionTaskFactory(
                episodeService,
                trackService,
                serializerFactory,
                publisher
        );
        factory.onSuccess(new TaskExecutedPacket<>(task, output));

        verify(trackService).setFromConversionResults(episode, output.tracks());
        verify(publisher).publishEvent(new MediaConversionCompletedEvent(episodeId));
    }

    private static Library libraryWithImports(Path imports) throws Exception {

        Library library = mock(Library.class);
        var resolver = mock(fr.anisekai.sanctum.interfaces.resolvers.StorageResolver.class);
        when(library.getResolver(Library.IMPORTS)).thenReturn(resolver);
        when(resolver.directory()).thenReturn(imports);
        return library;
    }

    private static Episode episode() {

        return episode(UUID.randomUUID());
    }

    private static Episode episode(UUID id) {

        Episode episode = new Episode();
        episode.setId(id);
        return episode;
    }

    private static MediaConversionInput input(UUID episodeId) {

        return new MediaConversionInput(
                new MediaConversionInput.Episode(episodeId, "episode.mkv", "0".repeat(64)),
                MediaConversionTaskFactory.DEFAULT_CONVERSION_OPTION
        );
    }

}
