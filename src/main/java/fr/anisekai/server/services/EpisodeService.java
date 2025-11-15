package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.repositories.EpisodeRepository;
import fr.anisekai.web.dto.worker.tasks.TrackCreationRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class EpisodeService extends AnisekaiService<Episode, Long, EpisodeRepository> {

    private final TrackService trackService;

    public EpisodeService(EpisodeRepository repository, EntityEventProcessor eventProcessor, TrackService trackService) {

        super(repository, eventProcessor);
        this.trackService = trackService;
    }

    public Optional<Episode> getEpisode(Anime anime, int number) {

        return this.getRepository().findByAnimeAndNumber(anime, number);
    }

    public Episode create(Anime anime, int number) {

        Episode episode = new Episode();
        episode.setAnime(anime);
        episode.setNumber(number);
        episode.setReady(false);

        return this.getRepository().save(episode);
    }

    public List<Episode> getAllReady() {

        return this.getRepository().findAllByReadyTrue();
    }

    @Transactional
    public Episode reset(long episodeId) {

        Episode episode = this.requireById(episodeId);
        episode.setReady(false);

        this.trackService.clearTracks(episode);
        return this.getRepository().save(episode);
    }

    @Transactional
    public Episode ready(long episodeId, Collection<TrackCreationRequest> newTracks) {

        Episode episode = this.reset(episodeId);

        List<Track> tracks = newTracks.stream().map(TrackCreationRequest::asTrack)
                                      .peek(track -> track.setEpisode(episode))
                                      .toList();

        this.trackService.getRepository().saveAll(tracks);

        episode.setReady(true);

        return this.getRepository().save(episode);
    }

}
