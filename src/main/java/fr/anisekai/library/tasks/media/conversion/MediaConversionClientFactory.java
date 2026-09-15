package fr.anisekai.library.tasks.media.conversion;

import fr.anisekai.core.serialization.JsonSerializerFactory;
import fr.anisekai.library.Library;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.wireless.tasks.conversion.AbstractMediaConversionClientFactory;
import fr.anisekai.wireless.tasks.conversion.MediaConversionInput;
import fr.anisekai.wireless.tasks.conversion.MediaConversionOutput;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class MediaConversionClientFactory extends AbstractMediaConversionClientFactory {

    private final Library            library;
    private final EpisodeService     episodeService;
    private final TorrentFileService torrentFileService;

    public MediaConversionClientFactory(JsonSerializerFactory serializerFactory, Library library, EpisodeService episodeService, TorrentFileService torrentFileService) {

        super(
                serializerFactory.createSerializer(MediaConversionInput.class),
                serializerFactory.createSerializer(MediaConversionOutput.class)
        );

        this.library            = library;
        this.episodeService     = episodeService;
        this.torrentFileService = torrentFileService;
    }

    @Override
    public @NotNull TaskHandler<MediaConversionInput, MediaConversionOutput> getHandler() {

        return new LocalMediaConversionHandler(this.library, this.episodeService, this.torrentFileService);
    }

}
