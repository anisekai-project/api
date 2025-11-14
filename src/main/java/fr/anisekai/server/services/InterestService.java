package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.keys.InterestKey;
import fr.anisekai.server.repositories.InterestRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class InterestService extends AnisekaiService<Interest, InterestKey, InterestRepository> {

    public InterestService(InterestRepository repository, EntityEventProcessor eventProcessor) {

        super(repository, eventProcessor);
    }

    public List<Interest> getInterests(Anime anime) {

        return this.getRepository().findByAnime(anime);
    }

    public List<Interest> getInterests(Collection<Anime> animes) {

        return this.getRepository().findByAnimeIn(animes);
    }

    public List<Interest> getInterests(DiscordUser user) {

        return this.getRepository().findByUser(user);
    }

    public void setInterest(DiscordUser user, Anime anime, byte level) {

        InterestKey key = new InterestKey(anime.getId(), user.getId());

        this.upsert(
                repo -> repo.findById(key),
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
