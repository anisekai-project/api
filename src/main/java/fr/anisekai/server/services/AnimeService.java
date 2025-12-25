package fr.anisekai.server.services;

import fr.anisekai.core.internal.plannifier.interfaces.ScheduleSpotData;
import fr.anisekai.core.persistence.UpsertResult;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.repositories.AnimeRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Service
public class AnimeService {

    private final AnimeRepository repository;

    public AnimeService(AnimeRepository repository) {

        this.repository = repository;
    }

    public AnimeRepository getRepository() {

        return this.repository;
    }

    /**
     * @deprecated Transition method, prefer declaring dedicated methods.
     */
    @Deprecated
    public Anime mod(long id, Consumer<Anime> updater) {

        return this.repository.mod(id, updater);
    }

    /**
     * @param id
     *         The entity identifier.
     *
     * @return The entity.
     */
    @Deprecated
    public Anime requireById(long id) {

        return this.repository.requireById(id);
    }

    @Deprecated
    @Transactional
    public UpsertResult<Anime> importAnime(DiscordUser sender, JSONObject source) {

        JSONArray genreArray = source.getJSONArray("genres");
        JSONArray themeArray = source.getJSONArray("themes");
        String    rawStatus  = source.getString("status");

        List<String> tagList = new ArrayList<>();
        genreArray.forEach(obj -> tagList.add(obj.toString()));
        themeArray.forEach(obj -> tagList.add(obj.toString()));

        String    name            = source.getString("title");
        String    synopsis        = source.getString("synopsis");
        AnimeList status          = AnimeList.from(rawStatus);
        String    link            = source.getString("link");
        String    image           = source.getString("image");
        int       total           = Integer.parseInt(source.getString("episode"));
        int       episodeDuration = Integer.parseInt(source.getString("time"));
        String    group           = source.getString("group");
        byte      order           = Byte.parseByte(source.getString("order"));

        return this.repository.upsert(
                () -> this.repository.findByUrl(link),
                () -> {
                    Anime anime = new Anime();
                    anime.setAddedBy(sender);
                    return anime;
                },
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

    public List<Anime> getAnimesAddedBy(DiscordUser user) {

        return this.repository.findByAddedBy(user);
    }

    public List<Anime> getOfStatus(AnimeList status) {

        return this.repository.findAllByList(status);
    }

    public List<Anime> getSimulcastsAvailable() {

        return this.getOfStatus(AnimeList.SIMULCAST_AVAILABLE);
    }

    public List<Anime> getAllDownloadable() {

        return this.repository.findAllByTitleRegexIsNotNull();
    }

    @Transactional
    public List<Anime> move(Collection<Long> ids, AnimeList to) {

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Anime> animes = this.repository.findAllById(ids);
        animes.forEach(anime -> anime.setList(to));
        return this.repository.saveAll(animes);
    }

    @Transactional
    public List<Anime> move(AnimeList from, AnimeList to) {

        List<Anime> animes = this.getOfStatus(from);
        animes.forEach(anime -> anime.setList(to));
        return this.repository.saveAll(animes);
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
