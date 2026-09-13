package fr.anisekai.library.tasks.media.conversion;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.library.Library;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.utils.IOUtils;
import fr.anisekai.wireless.tasks.conversion.MediaConversionInput;
import fr.anisekai.wireless.tasks.conversion.MediaConversionOutput;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Component
public class MediaConversionTaskFactory implements ServerFactory<Task, MediaConversionInput, MediaConversionOutput> {

    public static final MediaConversionInput.ConversionOptions DEFAULT_CONVERSION_OPTION = new MediaConversionInput.ConversionOptions(
            Codec.AAC,
            Codec.H264,
            Codec.SUBTITLE_COPY
    );

    private final EpisodeService            episodeService;
    private final TrackService              trackService;
    private final JsonSerializerFactory     serializerFactory;
    private final ApplicationEventPublisher eventPublisher;

    public MediaConversionTaskFactory(EpisodeService episodeService, TrackService trackService, JsonSerializerFactory serializerFactory, ApplicationEventPublisher eventPublisher) {

        this.episodeService    = episodeService;
        this.trackService      = trackService;
        this.serializerFactory = serializerFactory;
        this.eventPublisher    = eventPublisher;
    }

    public static MediaConversionInput createInput(Library library, Episode episode, Path source) {

        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(episode, "episode");
        Objects.requireNonNull(source, "source");

        try {
            Path imports        = library.getResolver(Library.IMPORTS).directory().toRealPath();
            Path resolvedSource = source.toRealPath();

            if (!resolvedSource.startsWith(imports) || resolvedSource.equals(imports) || !Files.isRegularFile(
                    resolvedSource)) {
                throw new IllegalArgumentException("Conversion source must be a regular file under Library.IMPORTS");
            }

            String sourceReference = imports.relativize(resolvedSource).toString();
            return new MediaConversionInput(
                    new MediaConversionInput.Episode(episode.getId(), sourceReference, IOUtils.hash(resolvedSource)),
                    DEFAULT_CONVERSION_OPTION
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to resolve media conversion source", e);
        }
    }

    @Override
    public @NotNull String getName() {

        return "media:convert";
    }

    @Override
    public @NotNull String getTaskName(@NotNull MediaConversionInput arguments) {

        MediaConversionInput.Episode episode = arguments.episode();
        return "%s:%s:%s".formatted(this.getName(), episode.id(), episode.sourceReference());
    }

    @Override
    public @NotNull ObjectSerializer<MediaConversionInput> getArgumentsSerializer() {

        return this.serializerFactory.createSerializer(MediaConversionInput.class);
    }

    @Override
    public @NotNull ObjectSerializer<MediaConversionOutput> getResultSerializer() {

        return this.serializerFactory.createSerializer(MediaConversionOutput.class);
    }

    @Override
    public void onSuccess(@NotNull TaskExecutedPacket<Task, MediaConversionOutput> packet) {

        MediaConversionInput input   = this.getArgumentsSerializer().deserialize(packet.task().getArguments());
        Episode              episode = this.episodeService.requireById(input.episode().id());

        this.trackService.setFromConversionResults(episode, packet.result().tracks());
        this.eventPublisher.publishEvent(new MediaConversionCompletedEvent(episode.getId()));
    }

}
