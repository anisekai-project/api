package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.repositories.EpisodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EpisodeService extends AnisekaiService<Episode, Long, EpisodeRepository> {

    public EpisodeService(EpisodeRepository repository, EntityEventProcessor eventProcessor) {

        super(repository, eventProcessor);
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

}
