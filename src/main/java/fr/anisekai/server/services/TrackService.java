package fr.anisekai.server.services;

import fr.anisekai.media.MediaFile;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.repositories.TrackRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrackService {

    private final TrackRepository repository;

    public TrackService(TrackRepository repository) {

        this.repository = repository;
    }

    public TrackRepository getRepository() {

        return this.repository;
    }

    /**
     * @param id
     *         The entity identifier.
     *
     * @return The entity.
     */
    @Deprecated
    public Track requireById(long id) {

        return this.repository.requireById(id);
    }

    public List<Track> getTracks(Episode episode) {

        return this.repository.findByEpisode(episode);
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
                    return this.repository.save(track);
                }).toList();
    }

    @Transactional
    public void clearTracks(Episode episode) {

        this.repository.deleteByEpisode(episode);
    }

}
