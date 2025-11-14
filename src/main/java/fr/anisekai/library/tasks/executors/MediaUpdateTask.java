package fr.anisekai.library.tasks.executors;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.validation.JsonObjectRule;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.library.Library;
import fr.anisekai.library.exceptions.NoMediaException;
import fr.anisekai.media.MediaFile;
import fr.anisekai.media.bin.FFMpeg;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.media.enums.Disposition;
import fr.anisekai.media.interfaces.MediaStreamMapper;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.tasking.TaskExecutor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MediaUpdateTask implements TaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaUpdateTask.class);

    public static final String OPTION_EPISODE = "episode";

    private final Library        library;
    private final EpisodeService service;

    public MediaUpdateTask(Library library, EpisodeService service) {

        this.library = library;
        this.service = service;
    }

    @Override
    public void validateParams(AnisekaiJson params) {

        params.validate(
                new JsonObjectRule(OPTION_EPISODE, true, int.class, Integer.class, long.class, Long.class)
        );
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) throws Exception {

        Episode episode = this.service.requireById(params.getLong(OPTION_EPISODE));

        AccessScope chunksScope   = new AccessScope(Library.CHUNKS, episode);
        AccessScope episodeScope  = new AccessScope(Library.EPISODES, episode);
        AccessScope subtitleScope = new AccessScope(Library.SUBTITLES, episode);

        Set<AccessScope> scopes      = Set.of(chunksScope, episodeScope, subtitleScope);
        Path             episodePath = this.library.resolve(episodeScope);

        if (!Files.isRegularFile(episodePath)) {
            throw new NoMediaException();
        }

        MediaFile media = MediaFile.of(episodePath);

        Map<Long, Track> trackMap = episode.getTracks()
                                           .stream()
                                           .collect(Collectors.toMap(Entity::getId, Function.identity()));

        MediaStreamMapper mapper = getStreamMapper(trackMap);

        try (IsolationSession context = this.library.createIsolation(scopes)) {

            Path scopedEpisodePath = context.resolve(episodeScope);

            FFMpeg.convert(media)
                  .copyVideo()
                  .copyAudio()
                  .copySubtitle()
                  .streamMapper(mapper)
                  .file(scopedEpisodePath)
                  .timeout(5, TimeUnit.MINUTES)
                  .run();

            MediaFile scopedMedia = MediaFile.of(scopedEpisodePath);

            Path chunkStorage = context.resolve(chunksScope);

            if (!Files.exists(chunkStorage)) Files.createDirectories(chunkStorage);

            FFMpeg.mdp(scopedMedia)
                  .into(chunkStorage)
                  .as("meta.mpd")
                  .timeout(5, TimeUnit.MINUTES)
                  .run();

            context.commit();
        }
    }

    private static @NotNull MediaStreamMapper getStreamMapper(Map<Long, Track> trackMap) {

        AtomicInteger counter = new AtomicInteger(0);

        return (binary, stream, codec) -> {

            if (!stream.getMetadata().containsKey("anisekai")) {
                throw new IllegalStateException("The stream " + stream.getId() + " is not mapped.");
            }

            long  trackId = Long.parseLong(stream.getMetadata().get("anisekai"));
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

}
