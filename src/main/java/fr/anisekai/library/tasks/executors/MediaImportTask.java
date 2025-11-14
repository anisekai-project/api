package fr.anisekai.library.tasks.executors;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.validation.JsonObjectRule;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.library.Library;
import fr.anisekai.media.MediaFile;
import fr.anisekai.media.MediaStream;
import fr.anisekai.media.bin.FFMpeg;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.media.enums.Disposition;
import fr.anisekai.media.interfaces.MediaStreamMapper;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.server.tasking.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaImportTask implements TaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaImportTask.class);

    private static final List<Codec> CODEC_EXCLUSION = List.of(Codec.MJPEG, Codec.PNG);
    private static final Codec       VIDEO_CODEC     = Codec.H264;
    private static final Codec       AUDIO_CODEC     = Codec.AAC;
    private static final Codec       SUBTITLE_CODEC  = Codec.SSA;

    public static final String OPTION_SOURCE  = "source";
    public static final String OPTION_EPISODE = "episode";
    public static final String OPTION_DELETE  = "delete";

    private final Library        library;
    private final TrackService   trackService;
    private final EpisodeService episodeService;

    private Episode   episode;
    private MediaFile media;
    private MediaFile converted;

    private AccessScope chunksScope;
    private AccessScope episodeScope;
    private AccessScope subtitleScope;

    private IsolationSession context;

    public MediaImportTask(Library library, TrackService trackService, EpisodeService episodeService) {

        this.library        = library;
        this.trackService   = trackService;
        this.episodeService = episodeService;
    }

    @Override
    public void validateParams(AnisekaiJson params) {

        params.validate(
                new JsonObjectRule(OPTION_SOURCE, true, String.class),
                new JsonObjectRule(OPTION_EPISODE, true, int.class, Integer.class, long.class, Long.class),
                new JsonObjectRule(OPTION_DELETE, false, boolean.class, Boolean.class)
        );
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) throws IOException, InterruptedException {

        Path    source = Path.of(params.getString(OPTION_SOURCE));
        boolean delete = params.optBoolean(OPTION_DELETE, false);

        this.episode = this.episodeService.requireById(params.getLong(OPTION_EPISODE));
        this.media   = MediaFile.of(source);

        String taskName = String.format("%s:%s", this.episode.getAnime().getId(), this.episode.getNumber());

        this.chunksScope   = new AccessScope(Library.CHUNKS, this.episode);
        this.episodeScope  = new AccessScope(Library.EPISODES, this.episode);
        this.subtitleScope = new AccessScope(Library.SUBTITLES, this.episode);

        Set<AccessScope> scopes = Set.of(this.chunksScope, this.episodeScope, this.subtitleScope);

        try (IsolationSession context = this.library.createIsolation(scopes)) {
            this.context = context;

            LOGGER.info("[{}] Converting media {}...", taskName, source);
            Path destination = this.convertFile();
            LOGGER.info("[{}] File converted to {}", taskName, destination);

            this.converted = MediaFile.of(destination);

            LOGGER.info("[{}] Creating MPD Meta and subtitle track files...", taskName);
            this.prepareFileForLibrary();
            LOGGER.info("[{}] Done.", taskName);

            LOGGER.info("[{}] Committing files to library...", taskName);
            context.commit();
            this.episodeService.mod(this.episode.getId(), entity -> entity.setReady(true));
            LOGGER.info("[{}] The media file has been imported.", taskName);
        }

        if (delete) {
            Files.delete(source);
        }
    }

    private Track createTrack(MediaStream stream, Codec codec) {

        Track entity = new Track();

        entity.setEpisode(this.episode);

        if (stream.getMetadata().containsKey("title")) {
            entity.setName(stream.getMetadata().get("title"));
        } else {
            entity.setName("Track %s".formatted(stream.getId()));
        }

        switch (codec.getType()) {
            case VIDEO -> entity.setCodec(VIDEO_CODEC);
            case AUDIO -> entity.setCodec(AUDIO_CODEC);
            case SUBTITLE -> entity.setCodec(SUBTITLE_CODEC);
        }

        entity.setCodec(codec);
        entity.setLanguage(stream.getMetadata().get("language"));
        entity.setDispositions(Disposition.toBits(stream.getDispositions()));

        return this.trackService.getRepository().save(entity);
    }

    private Path convertFile() throws IOException, InterruptedException {

        this.trackService.clearTracks(this.episode);
        AtomicInteger counter = new AtomicInteger(0);

        MediaStreamMapper mapper = ((MediaStreamMapper) (binary, stream, codec) -> {
            Track track = this.createTrack(stream, codec);

            binary.addArguments(
                    "-map",
                    String.format("0:%s", stream.getId())
            );

            binary.addArguments(
                    String.format("-c:%s", codec.getType().getChar()),
                    codec.getLibName()
            );

            binary.addArguments(
                    String.format("-metadata:s:%s", counter.getAndIncrement()),
                    String.format("anisekai=%s", track.getId())
            );

            if (codec.getType() == CodecType.VIDEO) {
                binary.addArguments("-crf", 25);
                binary.addArguments("-vf", "format=yuv420p");
            } else if (codec.getType() == CodecType.AUDIO) {
                binary.addArguments("-ac", "2");
            }

        }).onlyIf((stream, codec) -> !CODEC_EXCLUSION.contains(stream.getCodec()));

        return FFMpeg.convert(this.media)
                     .video(VIDEO_CODEC)
                     .audio(AUDIO_CODEC)
                     .subtitle(SUBTITLE_CODEC)
                     .streamMapper(mapper)
                     .file(this.context.resolve(this.episodeScope))
                     .timeout(3, TimeUnit.HOURS)
                     .run();
    }

    private void prepareFileForLibrary() throws IOException, InterruptedException {

        Path chunkStorage = this.context.resolve(this.chunksScope);
        Path subStorage   = this.context.resolve(this.subtitleScope);

        if (!Files.exists(chunkStorage)) Files.createDirectories(chunkStorage);
        if (!Files.exists(subStorage)) Files.createDirectories(subStorage);

        FFMpeg.mdp(this.converted)
              .into(chunkStorage)
              .as("meta.mpd")
              .timeout(5, TimeUnit.MINUTES)
              .run();

        FFMpeg.convert(this.converted)
              .noVideo()
              .noAudio()
              .subtitle(SUBTITLE_CODEC)
              .into(subStorage)
              .streamNamer((stream, codec) -> String.format(
                      "%s.%s",
                      stream.getMetadata().getOrDefault("anisekai", "-" + stream.getId()),
                      codec.getExtension()
              ))
              .split()
              .timeout(1, TimeUnit.HOURS)
              .run();
    }

}
