package fr.anisekai.server.services;

import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.repositories.EpisodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class EpisodeService {

    private final EpisodeRepository repository;

    public EpisodeService(EpisodeRepository repository, EntityEventProcessor eventProcessor) {

        this.repository = repository;
    }


    /**
     * @deprecated Transition method, prefer declaring dedicated methods.
     */
    @Deprecated
    public Episode mod(long id, Consumer<Episode> updater) {

        return this.repository.mod(id, updater);
    }

    /**
     * @param id
     *         The entity identifier.
     *
     * @return The entity.
     */
    @Deprecated
    public Episode requireById(long id) {

        return this.repository.requireById(id);
    }


    public Optional<Episode> getEpisode(Anime anime, int number) {

        return this.repository.findByAnimeAndNumber(anime, number);
    }

    public Episode create(Anime anime, int number) {

        Episode episode = new Episode();
        episode.setAnime(anime);
        episode.setNumber(number);
        episode.setReady(false);

        return this.repository.save(episode);
    }

    public List<Episode> getAllReady() {

        return this.repository.findAllByReadyTrue();
    }

}
