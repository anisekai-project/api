package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.media.MediaFile;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.repositories.TrackRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrackService extends AnisekaiService<Track, Long, TrackRepository> {

    public TrackService(TrackRepository repository, EntityEventProcessor eventProcessor) {

        super(repository, eventProcessor);
    }

    public List<Track> getTracks(Episode episode) {

        return this.getRepository().findByEpisode(episode);
    }

    @Transactional
    public List<Track> createFromMediaTrack(Episode episode, MediaFile mediaFile) {

        return mediaFile
                .getStreams()
                .stream()
                .map(stream -> {
                    Track track = new Track();
                    track.setEpisode(episode);
                    track.setName("Track " + stream.getId());
                    track.setCodec(stream.getCodec());
                    track.setLanguage(stream.getMetadata().get("language"));
                    return this.getRepository().save(track);
                }).toList();
    }

    @Transactional
    public void clearTracks(Episode episode) {

        this.getRepository().deleteByEpisode(episode);
    }

}
