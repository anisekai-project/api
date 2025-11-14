package fr.anisekai.discord.interactions.administation;

import fr.alexpado.jda.interactions.annotations.Interact;
import fr.alexpado.jda.interactions.annotations.Option;
import fr.alexpado.jda.interactions.annotations.Param;
import fr.alexpado.jda.interactions.responses.SlashResponse;
import fr.anisekai.discord.annotations.InteractionBean;
import fr.anisekai.discord.exceptions.RequireAdministratorException;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.discord.tasks.watchlist.create.WatchlistCreateFactory;
import fr.anisekai.discord.tasks.watchlist.update.WatchlistUpdateFactory;
import fr.anisekai.library.Library;
import fr.anisekai.library.tasks.factories.MediaImportFactory;
import fr.anisekai.library.tasks.factories.MediaUpdateFactory;
import fr.anisekai.library.tasks.factories.TorrentRetentionControlFactory;
import fr.anisekai.library.tasks.factories.TorrentSourcingFactory;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.SettingService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.utils.DiscordUtils;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@InteractionBean
public class TaskInteractions {

    private final Library        library;
    private final AnimeService   animeService;
    private final EpisodeService episodeService;
    private final TaskService    service;
    private final SettingService settingService;

    public TaskInteractions(Library library, AnimeService animeService, EpisodeService episodeService, TaskService service, SettingService settingService) {

        this.library        = library;
        this.animeService   = animeService;
        this.episodeService = episodeService;
        this.service        = service;
        this.settingService = settingService;
    }

    private static void requireAdministrator(DiscordUser user) {

        if (!user.isAdministrator()) {
            throw new RequireAdministratorException();
        }
    }

    // <editor-fold desc="@ task/check-downloads ─ Check for new episode to automatically download">
    @Interact(
            name = "task/check-downloads",
            description = "\uD83D\uDD12 — Vérifie si des épisodes peuvent être téléchargés.",
            options = {
                    @Option(
                            name = "url",
                            description = "URL source à utiliser au lieu de l'url configurée",
                            type = OptionType.STRING
                    )
            }
    )
    public SlashResponse checkDownloads(DiscordUser user, @Param("url") String url) {

        requireAdministrator(user);

        Optional<String> optionalUserSource    = Optional.ofNullable(url);
        Optional<String> optionalDefaultSource = this.settingService.getDownloadSource();

        if (optionalUserSource.isPresent()) {
            this.service.getFactory(TorrentSourcingFactory.class).queue(optionalUserSource.get());
            return DiscordResponse.success("La vérification va être effectuée sous peu.");
        }

        if (optionalDefaultSource.isPresent()) {
            this.service.getFactory(TorrentSourcingFactory.class).queue(optionalDefaultSource.get());
            return DiscordResponse.success("La vérification va être effectuée sous peu.");
        }

        return DiscordResponse.error("Aucune source disponible pour le téléchargement automatique.");
    }
    // </editor-fold>

    // <editor-fold desc="@ task/reset-list ─ Create the watchlists within the configured channel.">
    @Interact(
            name = "task/reset-list",
            description = "\uD83D\uDD12 — Créé les watchlist dans le salon configuré."
    )
    public SlashResponse resetLists(DiscordUser user) {

        this.service.getFactory(WatchlistCreateFactory.class).queue(Task.PRIORITY_MANUAL_HIGH);
        return DiscordResponse.success(
                "Les listes ont été réinitialisée. Il vous faudra supprimer les messages des anciennes listes.");
    }
    // </editor-fold>

    // <editor-fold desc="@ task/refresh-lists ─ Refresh all watchlists.">
    @Interact(
            name = "task/refresh-lists",
            description = "\uD83D\uDD12 — Force l'actualisation des listes."
    )
    public SlashResponse refreshLists(DiscordUser user) {

        requireAdministrator(user);

        WatchlistUpdateFactory factory = this.service.getFactory(WatchlistUpdateFactory.class);

        for (AnimeList list : AnimeList.collect(AnimeList.Property.SHOW)) {
            factory.queue(list, Task.PRIORITY_MANUAL_HIGH);
        }

        return DiscordResponse.success("Les listes vont être actualisée.");
    }
    // </editor-fold>

    // <editor-fold desc="@ task/purge-torrents ─ Delete the torrent files following the retention setting.">
    @Interact(
            name = "task/purge-torrents",
            description = "\uD83D\uDD12 — Supprime les fichiers de torrents selon l'option de rétention."
    )
    public SlashResponse purgeTorrents(DiscordUser user) {

        requireAdministrator(user);
        this.service.getFactory(TorrentRetentionControlFactory.class).queue();
        return DiscordResponse.success("Les torrents vont être nettoyés.");
    }
    // </editor-fold>

    // <editor-fold desc="@ task/import-file ─ Import a media file as an episode for an anime">
    @Interact(
            name = "task/import-file",
            description = "\uD83D\uDD12 — Importe un fichier media en tant qu'épisode d'un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel le fichier sera importé",
                            type = OptionType.INTEGER,
                            required = true,
                            autoComplete = true
                    ),
                    @Option(
                            name = "file",
                            autoCompleteName = "importable:files",
                            description = "Fichier à importer",
                            type = OptionType.STRING,
                            required = true,
                            autoComplete = true
                    ),
                    @Option(
                            name = "episode",
                            description = "Numéro de l'épisode correspondant au fichier",
                            type = OptionType.INTEGER,
                            required = true
                    )
            }
    )
    public SlashResponse importMedia(DiscordUser user, @Param("anime") long animeId, @Param("file") String file, @Param("episode") long episodeNumber) {

        requireAdministrator(user);
        Anime anime  = this.animeService.requireById(animeId);
        Path  source = this.library.getResolver(Library.IMPORTS).file(file);

        if (!Files.isRegularFile(source)) {
            return DiscordResponse.error("Le fichier choisi n'est pas valide.");
        }

        long max = Math.abs(anime.getTotal());

        if (episodeNumber > max) {
            return DiscordResponse.warn("Le nombre d'épisode maximum pour cet anime est de **%s**", max);
        }

        Episode episode = anime.getEpisodes()
                               .stream()
                               .filter(item -> item.getNumber() == episodeNumber)
                               .findFirst()
                               .orElseGet(() -> this.episodeService.create(anime, (int) episodeNumber));

        Task task = this.service.getFactory(MediaImportFactory.class).queue(source, episode);

        return DiscordResponse.info(
                "L'épisode **%s** de l'anime **%s** va être importé.\n%s",
                episode.getNumber(),
                DiscordUtils.link(anime),
                task.toDiscordName()
        );
    }
    // </editor-fold>

    // <editor-fold desc="@ task/import-directory ─ Import a media directory where each file is an episode for an anime">
    @Interact(
            name = "task/import-directory",
            description = "\uD83D\uDD12 — Importe un dossier media où chaque fichier correspond à un épisode d'un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel le fichier sera importé",
                            type = OptionType.INTEGER,
                            required = true,
                            autoComplete = true
                    ),
                    @Option(
                            name = "directory",
                            autoCompleteName = "importable:directories",
                            description = "Dossier à importer",
                            type = OptionType.STRING,
                            required = true,
                            autoComplete = true
                    ),
            }
    )
    public SlashResponse importMedia(DiscordUser user, @Param("anime") long animeId, @Param("directory") String directory) throws Exception {

        requireAdministrator(user);
        Anime anime  = this.animeService.requireById(animeId);
        Path  source = this.library.getResolver(Library.IMPORTS).directory(directory);

        if (!Files.isDirectory(source)) {
            return DiscordResponse.error("Le dossier choisi n'est pas valide.");
        }

        // Analyzing content
        Map<Integer, Path> pathMapping = new HashMap<>();
        Pattern            pattern     = Pattern.compile("(?<ep>\\d+)\\.(mkv|mp4)");
        int                max         = Math.abs(anime.getTotal());

        try (Stream<Path> stream = Files.list(source)) {
            for (Path path : stream.toList()) {
                String  name    = path.getFileName().toString();
                Matcher matcher = pattern.matcher(name);

                if (!matcher.matches()) {
                    return DiscordResponse.error(
                            "Impossible de determiner le numéro d'épisode pour le fichier `%s`.\nPattern: `%s`",
                            name,
                            pattern.pattern()
                    );
                }

                int episodeNumber = Integer.parseInt(matcher.group("ep"));

                if (episodeNumber > max) {
                    return DiscordResponse.warn(
                            "Le nombre d'épisode maximum pour cet anime est de **%s**.\nÉpisode détecté: **%s**",
                            max,
                            episodeNumber
                    );
                }

                pathMapping.put(episodeNumber, path.toAbsolutePath().normalize());
            }
        }

        List<Task> tasks = new ArrayList<>();
        for (Map.Entry<Integer, Path> entry : pathMapping.entrySet()) {
            Episode episode = anime.getEpisodes()
                                   .stream()
                                   .filter(item -> item.getNumber() == entry.getKey())
                                   .findFirst()
                                   .orElseGet(() -> this.episodeService.create(anime, entry.getKey()));

            Task task = this.service.getFactory(MediaImportFactory.class).queue(entry.getValue(), episode);
            tasks.add(task);
        }

        return DiscordResponse.info(
                "Le dossier `%s` va être importé. **%s** tâches ont été créé.\n- %s",
                directory,
                tasks.size(),
                tasks.stream().map(Task::toDiscordName).collect(Collectors.joining("\n- "))
        );
    }
    // </editor-fold>

    // <editor-fold desc="@ task/update-episode ─ Refresh an episode on the disk">
    @Interact(
            name = "task/update-episode",
            description = "\uD83D\uDD12 — Actualise un épisode sur le disque.",
            options = {
                    @Option(
                            name = "episode",
                            autoComplete = true,
                            description = "Episode pour lequel l'actualisation sera lancée.",
                            type = OptionType.INTEGER,
                            required = true
                    )
            }
    )
    public SlashResponse updateEpisode(DiscordUser user, @Param("episode") long episodeId) {

        requireAdministrator(user);
        Episode episode = this.episodeService.requireById(episodeId);
        Task    task    = this.service.getFactory(MediaUpdateFactory.class).queue(episode);

        return DiscordResponse.info(
                "L'épisode **%s** de l'anime **%s** va être actualisé.\n%s",
                episode.getNumber(),
                DiscordUtils.link(episode.getAnime()),
                task.toDiscordName()
        );
    }
    // </editor-fold>

}
