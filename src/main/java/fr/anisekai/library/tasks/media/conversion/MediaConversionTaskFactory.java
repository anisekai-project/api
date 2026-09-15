package fr.anisekai.library.tasks.media.conversion;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.library.Library;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.server.tasking.IsolatedServerFactory;
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
import java.util.Set;

@Component
public class MediaConversionTaskFactory implements ServerFactory<Task, MediaConversionInput, MediaConversionOutput>, IsolatedServerFactory<MediaConversionInput> {

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
                    new MediaConversionInput.Episode(
                            episode.getId(),
                            new MediaConversionInput.Source(MediaConversionInput.Store.IMPORTS, sourceReference),
                            IOUtils.hash(resolvedSource)
                    ),
                    DEFAULT_CONVERSION_OPTION
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to resolve media conversion source", e);
        }
    }

    public static MediaConversionInput createInput(Library library, Episode episode, TorrentFile torrentFile) {

        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(episode, "episode");
        Objects.requireNonNull(torrentFile, "torrentFile");

        Path source = library.findDownload(torrentFile)
                             .orElseThrow(() -> new IllegalArgumentException(
                                     "Conversion source must be a downloaded file resolvable via Library.findDownload"));

        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Conversion source must be a regular file under Library.DOWNLOADS");
        }

        try {
            Path realSource = source.toRealPath();
            if (!realSource.startsWith(library.getResolver(Library.DOWNLOADS).directory().toRealPath())) {
                throw new IllegalArgumentException("Conversion source escapes Library.DOWNLOADS");
            }
            return new MediaConversionInput(
                    new MediaConversionInput.Episode(
                            episode.getId(),
                            new MediaConversionInput.Source(
                                    MediaConversionInput.Store.DOWNLOADS,
                                    "%s/%d".formatted(torrentFile.getTorrent().getId(), torrentFile.getIndex())
                            ),
                            IOUtils.hash(source)
                    ),
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

        return "%s:%s".formatted(this.getName(), arguments.episode().id());
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
    public @NotNull Set<AccessScope> getIsolationScopes(@NotNull Task task, @NotNull MediaConversionInput input) {

        Episode episode = this.episodeService.requireById(input.episode().id());
        return Set.of(new AccessScope(Library.EPISODES, episode.getScopedName()));
    }

    @Override
    public void onSuccess(@NotNull TaskExecutedPacket<Task, MediaConversionOutput> packet) {

        MediaConversionInput input   = this.getArgumentsSerializer().deserialize(packet.task().getArguments());
        Episode              episode = this.episodeService.requireById(input.episode().id());

        this.trackService.setFromConversionResults(episode, packet.result().tracks());
        this.eventPublisher.publishEvent(new MediaConversionCompletedEvent(episode.getId()));
    }

}
