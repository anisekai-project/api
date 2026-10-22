package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.core.persistence.UpsertResult;
import fr.anisekai.scheduler.event.interfaces.ScheduleSpotData;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.exceptions.anime.AnimePermissionException;
import fr.anisekai.server.repositories.AnimeRepository;
import fr.anisekai.web.dto.AnimeImportRequest;
import fr.anisekai.web.dto.AnimeRequestData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;

@Service
public class AnimeService extends AnisekaiService<Anime, UUID, AnimeRepository> {

    public AnimeService(AnimeRepository repository, EntityEventProcessor eventProcessor) {

        super(repository, eventProcessor);
    }

    private Consumer<Anime> applyRequestData(AnimeRequestData data) {

        return anime -> {
            anime.setGroup(data.group());
            anime.setOrder(data.order());
            anime.setTitle(data.title());
            anime.setList(data.list());
            anime.setSynopsis(data.synopsis());
            anime.setTags(data.tags());
            anime.setThumbnailUrl(data.image());
            anime.setUrl(data.link());
            anime.setTotal(data.total());
            anime.setEpisodeDuration(data.episodeDuration());
        };
    }

    @Transactional
    public Anime createAnime(DiscordUser sender, AnimeRequestData data) {

        this.forbid(repository -> repository.findByUrl(data.link()));
        return this.create(() -> new Anime(sender), this.applyRequestData(data));
    }

    @Transactional
    public Anime updateAnime(DiscordUser sender, UUID id, AnimeRequestData data) {

        Anime anime = this.requireById(id);

        if (!sender.isAdministrator()) {
            throw new AnimePermissionException("You don't have the permission required to modify this anime.");
        }

        this.applyRequestData(data).accept(anime);
        return this.getRepository().save(anime);
    }

    @Deprecated
    @Transactional
    public UpsertResult<Anime> importAnime(DiscordUser sender, AnimeImportRequest request) {

        List<String> tagList = new ArrayList<>();
        if (request.genres() != null) tagList.addAll(request.genres());
        if (request.themes() != null) tagList.addAll(request.themes());

        String    name            = request.title();
        String    synopsis        = request.synopsis();
        AnimeList status          = AnimeList.from(request.status());
        String    link            = request.link();
        String    image           = request.image();
        int       total           = request.episode();
        int       episodeDuration = request.time();
        String    group           = request.group();
        byte      order           = request.order();

        return this.upsert(
                repository -> repository.findByUrl(link),
                () -> new Anime(sender),
                anime -> {
                    anime.setGroup(group);
                    anime.setOrder(order);
                    anime.setTitle(name);
                    anime.setList(status);
                    anime.setSynopsis(synopsis);
                    anime.setTags(tagList);
                    anime.setThumbnailUrl(image);
                    anime.setUrl(link);
                    anime.setTotal(total);
                    anime.setEpisodeDuration(episodeDuration);
                }
        );
    }

    public List<Anime> getOfStatus(AnimeList status) {

        return this.getRepository().findAllByList(status);
    }

    public List<Anime> getSimulcastsAvailable() {

        return this.getOfStatus(AnimeList.SIMULCAST_AVAILABLE);
    }

    public List<Anime> getAllDownloadable() {

        return this.getRepository().findAllByTitleRegexIsNotNull();
    }

    @Transactional
    public List<Anime> move(Collection<UUID> ids, AnimeList to) {

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Anime> animes = this.getRepository().findAllById(ids);
        animes.forEach(anime -> anime.setList(to));
        return this.getRepository().saveAll(animes);
    }

    @Transactional
    public List<Anime> move(AnimeList from, AnimeList to) {

        List<Anime> animes = this.getOfStatus(from);
        animes.forEach(anime -> anime.setList(to));
        return this.getRepository().saveAll(animes);
    }

    public Consumer<Anime> defineProgression(int progression) {

        return entity -> {
            entity.setWatched(progression);
            if (entity.getTotal() == progression) {
                entity.setList(AnimeList.WATCHED);
            }
        };
    }

    public Consumer<Anime> defineProgression(int progression, int total) {

        return entity -> {
            entity.setTotal(total);
            this.defineProgression(progression).accept(entity);
        };
    }

    public Consumer<Anime> defineWatching() {

        return anime -> {
            switch (anime.getList()) {
                case WATCHED,
                     DOWNLOADED,
                     DOWNLOADING,
                     NOT_DOWNLOADED,
                     NO_SOURCE,
                     UNAVAILABLE,
                     CANCELLED -> anime.setList(AnimeList.WATCHING);
                case SIMULCAST_AVAILABLE -> anime.setList(AnimeList.SIMULCAST);
            }
        };
    }

    public Consumer<Anime> defineScheduleProgress(ScheduleSpotData<?> broadcast) {

        return entity -> this.defineProgression(entity.getWatched() + broadcast.getEpisodeCount()).accept(entity);
    }

}
