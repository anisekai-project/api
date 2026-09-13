package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.repositories.TrackRepository;
import fr.anisekai.wireless.tasks.conversion.MediaConversionOutput;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class TrackService extends AnisekaiService<Track, UUID, TrackRepository> {

    public TrackService(TrackRepository repository, EntityEventProcessor eventProcessor) {

        super(repository, eventProcessor);
    }

    public List<Track> getTracks(Episode episode) {

        return this.getRepository().findByEpisode(episode);
    }

    @Transactional
    public List<Track> setFromConversionResults(Episode episode, Collection<MediaConversionOutput.Track> tracks) {

        this.clearTracks(episode);

        return tracks.stream().map(item -> {
            Track track = new Track();
            track.setId(item.uuid());
            track.setEpisode(episode);
            track.setName(item.name());
            track.setCodec(item.codec());
            track.setLanguage(item.language());
            track.setDispositions(item.dispositions());
            return this.getRepository().save(track);
        }).toList();
    }

    @Transactional
    public void clearTracks(Episode episode) {

        this.getRepository().deleteByEpisode(episode);
    }

}
