package fr.anisekai.library.tasks.torrent.sourcing;

import fr.anisekai.core.internal.services.Nyaa;
import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.library.services.SpringTransmissionClient;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Torrent;
import fr.anisekai.server.domain.entities.TorrentFile;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TorrentFileService;
import fr.anisekai.server.services.TorrentService;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TorrentSourcingTask implements TaskHandler<TorrentSourcingTaskInput, Nothing> {

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
    public @NonNull Nothing handle(@NonNull TorrentSourcingTaskInput arguments) throws Exception {

        SpringTransmissionClient client = this.torrentService.getClient();
        client.check();
        if (!client.isAvailable()) throw new IllegalArgumentException("Transmission client is not available");


        List<Anime> animes = this.animeService.getAllDownloadable();
        Map<UUID, Anime> animeMap = animes
                .stream()
                .collect(Collectors.toMap(Anime::getId, Function.identity()));

        Map<UUID, Pattern> regexMap = animes
                .stream()
                .filter(anime -> anime.getTitleRegex() != null)
                .collect(Collectors.toMap(Anime::getId, Anime::getTitleRegex));

        URI              uri     = URI.create(arguments.feed());
        List<Nyaa.Entry> entries = Nyaa.fetch(uri);

        for (Nyaa.Entry entry : entries) {

            for (Map.Entry<UUID, Pattern> mapEntry : regexMap.entrySet()) {
                Matcher matcher = mapEntry.getValue().matcher(entry.title());
                if (matcher.find()) {
                    Anime anime  = animeMap.get(mapEntry.getKey());
                    int   number = Integer.parseInt(matcher.group("ep"));

                    Optional<Episode> optionalEpisode = this.episodeService.getEpisode(anime, number);

                    Transmission.Torrent query = client.query(entry);
                    if (query.files().size() > 1) {
                        client.delete(query);
                        throw new IllegalStateException("Not supporting multi-file download.");
                    }

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

                    Transmission.Torrent transmissionTorrent = client.resume(query);

                    Torrent torrent = this.createTorrent(entity -> {
                        entity.setHash(transmissionTorrent.hash());
                        entity.setName(entry.title());
                        entity.setStatus(transmissionTorrent.status());
                        entity.setProgress(transmissionTorrent.percentDone());
                        entity.setLink(entry.link());
                        entity.setPriority(arguments.priority());
                        entity.setDownloadDirectory(transmissionTorrent.downloadDir());
                    });

                    String file = transmissionTorrent.files().getFirst();

                    this.createTorrentFile(entity -> {
                        entity.setEpisode(episode);
                        entity.setTorrent(torrent);
                        entity.setIndex(0);
                        entity.setName(file);
                    });

                    break;
                }
            }
        }

        return Nothing.INSTANCE;
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
