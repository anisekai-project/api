package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.keys.InterestKey;
import fr.anisekai.server.repositories.InterestRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class InterestService {

    private final InterestRepository repository;

    public InterestService(InterestRepository repository) {

        this.repository = repository;
    }

    public List<Interest> getInterests(Anime anime) {

        return this.repository.findByAnime(anime);
    }

    public List<Interest> getInterests(Collection<Anime> animes) {

        return this.repository.findByAnimeIn(animes);
    }

    public List<Interest> getInterests(DiscordUser user) {

        return this.repository.findByUser(user);
    }

    public void setInterest(DiscordUser user, Anime anime, byte level) {

        InterestKey key = new InterestKey(anime.getId(), user.getId());

        this.repository.upsert(
                () -> this.repository.findById(key),
                () -> {
                    Interest entity = new Interest();
                    entity.setUser(user);
                    entity.setAnime(anime);
                    return entity;
                },
                entity -> entity.setLevel(level)
        );
    }

}
