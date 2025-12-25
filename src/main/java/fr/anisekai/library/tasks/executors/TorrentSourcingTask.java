package fr.anisekai.library.tasks.executors;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.validation.JsonObjectRule;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.core.internal.services.Nyaa;
import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.library.services.SpringTransmissionClient;
import fr.anisekai.server.domain.entities.*;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.server.services.TorrentService;
import fr.anisekai.server.tasking.TaskExecutor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TorrentSourcingTask implements TaskExecutor {

    public static final String OPTION_SOURCE = "source";

    private final AnimeService       animeService;
    private final EpisodeService     episodeService;
    private final TorrentService     torrentService;
    private final TorrentFileService torrentFileService;

    public TorrentSourcingTask(AnimeService animeService, EpisodeService episodeService, TorrentService torrentService, TorrentFileService torrentFileService) {

        this.animeService       = animeService;
        this.episodeService     = episodeService;
        this.torrentService     = torrentService;
        this.torrentFileService = torrentFileService;
    }

    @Override
    public void validateParams(AnisekaiJson params) {

        params.validate(
                new JsonObjectRule(OPTION_PRIORITY, false, int.class, long.class, Integer.class, Long.class),
                new JsonObjectRule(OPTION_SOURCE, true, String.class)
        );
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) throws Exception {

        byte   priority = params.has(OPTION_PRIORITY) ? (byte) params.getInt(OPTION_PRIORITY) : Task.PRIORITY_AUTOMATIC_LOW;
        String source   = params.getString(OPTION_SOURCE);

        timer.action("client-check", "Check the transmission client");
        SpringTransmissionClient client = this.torrentService.getClient();
        client.check();
        if (!client.isAvailable()) throw new IllegalArgumentException("Transmission client is not available");
        timer.endAction();

        timer.action("load-entities", "Load entities from database");
        List<Anime> animes = this.animeService.getAllDownloadable();
        Map<Long, Anime> animeMap = animes
                .stream()
                .collect(Collectors.toMap(Anime::getId, Function.identity()));

        Map<Long, Pattern> regexMap = animes
                .stream()
                .filter(anime -> anime.getTitleRegex() != null)
                .collect(Collectors.toMap(Anime::getId, Anime::getTitleRegex));
        timer.endAction();

        timer.action("rss-load", "Reading RSS content");
        URI              uri     = URI.create(source);
        List<Nyaa.Entry> entries = Nyaa.fetch(uri);
        timer.endAction();

        timer.action("rss-handle", "Handling RSS content");
        for (Nyaa.Entry entry : entries) {
            timer.action("find-rss-match", entry.link());

            for (Map.Entry<Long, Pattern> mapEntry : regexMap.entrySet()) {
                Matcher matcher = mapEntry.getValue().matcher(entry.title());
                if (matcher.find()) {
                    Anime anime  = animeMap.get(mapEntry.getKey());
                    int   number = Integer.parseInt(matcher.group("ep"));

                    Optional<Episode> optionalEpisode = this.episodeService.getEpisode(anime, number);

                    timer.action("offering-torrent", "Add torrent to client");
                    Transmission.Torrent query = client.query(entry);
                    if (query.files().size() > 1) {
                        client.delete(query);
                        throw new IllegalStateException("Not supporting multi-file download.");
                    }
                    timer.endAction();

                    Episode episode;

                    if (optionalEpisode.isPresent()) {
                        episode = optionalEpisode.get();
                        Optional<TorrentFile> file = this.torrentFileService.getFile(episode);

                        if (file.isPresent()) {
                            break; // Already downloading this file.
                        }

                    } else {
                        episode = this.episodeService.create(anime, number);
                    }

                    timer.action("starting-torrent", "Starting torrent");
                    Transmission.Torrent transmissionTorrent = client.resume(query);
                    timer.endAction();

                    timer.action("saving-entities", "Saving entities to database");
                    Torrent torrent = this.createTorrent(entity -> {
                        entity.setHash(transmissionTorrent.hash());
                        entity.setName(entry.title());
                        entity.setStatus(transmissionTorrent.status());
                        entity.setProgress(transmissionTorrent.percentDone());
                        entity.setLink(entry.link());
                        entity.setPriority(priority);
                        entity.setDownloadDirectory(transmissionTorrent.downloadDir());
                    });

                    String file = transmissionTorrent.files().getFirst();

                    this.createTorrentFile(entity -> {
                        entity.setEpisode(episode);
                        entity.setTorrent(torrent);
                        entity.setIndex(0);
                        entity.setName(file);
                    });

                    timer.endAction();
                    break;
                }
            }
            timer.endAction();
        }
        timer.endAction();
    }

    private Torrent createTorrent(Consumer<Torrent> consumer) {

        Torrent torrent = new Torrent();
        consumer.accept(torrent);
        return this.torrentService.getRepository().save(torrent);
    }

    private TorrentFile createTorrentFile(Consumer<TorrentFile> consumer) {

        TorrentFile file = new TorrentFile();
        consumer.accept(file);
        return this.torrentFileService.getRepository().save(file);
    }

}
