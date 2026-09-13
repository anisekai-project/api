package fr.anisekai.library.tasks.media.store;

import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.library.Library;
import fr.anisekai.library.exceptions.NoMediaException;
import fr.anisekai.media.MediaFile;
import fr.anisekai.media.bin.FFMpeg;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.media.enums.Disposition;
import fr.anisekai.media.interfaces.MediaStreamMapper;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.services.EpisodeService;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MediaStoreTask implements TaskHandler<MediaStoreTaskInput, Nothing> {

    private final Library        library;
    private final EpisodeService service;

    public MediaStoreTask(Library library, EpisodeService service) {

        this.library = library;
        this.service = service;
    }

    private static @NotNull MediaStreamMapper getStreamMapper(Map<UUID, Track> trackMap) {

        AtomicInteger counter = new AtomicInteger(0);

        return (binary, stream, codec) -> {

            if (!stream.getMetadata().containsKey("anisekai")) {
                throw new IllegalStateException("The stream " + stream.getId() + " is not mapped.");
            }

            UUID  trackId = UUID.fromString(stream.getMetadata().get("anisekai"));
            Track track   = trackMap.get(trackId);
            if (track == null) {
                throw new IllegalStateException("The stream " + stream.getId() + " is mapped to an unknown track.");
            }

            int       id   = counter.getAndIncrement();
            CodecType type = codec.getType();

            binary.addArguments("-map", String.format("0:%d", stream.getId()));
            binary.addArguments(String.format("-c:%d", id), codec.getLibName());

            // Set language metadata (only for audio and subtitle)
            if (type == CodecType.AUDIO || type == CodecType.SUBTITLE) {
                String lang = track.getLanguage();
                if (lang != null && !lang.isBlank()) {
                    binary.addArguments(String.format("-metadata:s:%d", id), "language=" + lang);
                }

                binary.addArguments(String.format("-metadata:s:%d", id), "title=" + track.getName());
            }

            binary.addArguments(String.format("-metadata:s:%d", id), "anisekai=" + track.getId());

            String dispositions = Disposition
                    .fromBits(track.getDispositions())
                    .stream()
                    .map(Disposition::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.joining("+"));

            binary.addArguments(
                    String.format("-disposition:%d", id),
                    String.join("+", dispositions.isEmpty() ? "0" : dispositions)
            );
        };
    }

    @Override
    public @NonNull Nothing handle(@NonNull MediaStoreTaskInput arguments) throws Exception {

        Episode episode = this.service.requireById(arguments.episode());

        AccessScope episodeScope  = new AccessScope(Library.EPISODES, episode.getScopedName());
        AccessScope chunksScope   = new AccessScope(Library.CHUNKS, episode.getScopedName());
        AccessScope subtitleScope = new AccessScope(Library.SUBTITLES, episode.getScopedName());

        Path episodePath = this.library.resolve(episodeScope);

        if (!Files.isRegularFile(episodePath)) {
            throw new NoMediaException();
        }

        Set<AccessScope> scopes;

        if (arguments.refresh()) {
            scopes = Set.of(episodeScope, chunksScope, subtitleScope);
        } else {
            scopes = Set.of(chunksScope, subtitleScope);
        }

        try (IsolationSession storage = this.library.createIsolation(scopes)) {
            MediaFile activeMedia = MediaFile.of(episodePath);

            if (arguments.refresh()) {
                MediaFile media = MediaFile.of(episodePath);

                Map<UUID, Track> trackMap = episode.getTracks()
                                                   .stream()
                                                   .collect(Collectors.toMap(Entity::getId, Function.identity()));

                MediaStreamMapper mapper = getStreamMapper(trackMap);

                Path scopedEpisodePath = storage.resolve(episodeScope);

                FFMpeg.convert(media)
                      .copyVideo()
                      .copyAudio()
                      .copySubtitle()
                      .streamMapper(mapper)
                      .file(scopedEpisodePath)
                      .timeout(5, TimeUnit.MINUTES)
                      .run();

                activeMedia = MediaFile.of(scopedEpisodePath);
            }

            Path chunkStorage = storage.resolve(chunksScope);

            if (!Files.exists(chunkStorage)) Files.createDirectories(chunkStorage);

            FFMpeg.mdp(activeMedia)
                  .into(chunkStorage)
                  .as("meta.mpd")
                  .timeout(5, TimeUnit.MINUTES)
                  .run();

            storage.commit();
        }

        return Nothing.INSTANCE;
    }

}
