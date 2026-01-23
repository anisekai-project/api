package fr.anisekai.discord.interactions.task;

import fr.alexpado.interactions.annotations.Completion;
import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.completions.AnimeCompletion;
import fr.anisekai.discord.completions.ImportableDirectoryCompletion;
import fr.anisekai.discord.completions.ImportableFileCompletion;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.library.Library;
import fr.anisekai.library.tasks.factories.MediaImportFactory;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.utils.DiscordUtils;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DiscordBean
@RequireAdmin
public class ImportTaskSlashInteraction {

    private final TaskService    service;
    private final Library        library;
    private final AnimeService   animeService;
    private final EpisodeService episodeService;

    public ImportTaskSlashInteraction(TaskService service, Library library, AnimeService animeService, EpisodeService episodeService) {

        this.service        = service;
        this.library        = library;
        this.animeService   = animeService;
        this.episodeService = episodeService;
    }

    @Slash(
            name = "task/import/file",
            description = "\uD83D\uDD12 — Importe un fichier media en tant qu'épisode d'un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel le fichier sera importé",
                            type = OptionType.INTEGER,
                            required = true,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "file",
                            description = "Fichier à importer",
                            type = OptionType.STRING,
                            required = true,
                            completion = @Completion(named = ImportableFileCompletion.NAME)
                    ),
                    @Option(
                            name = "episode",
                            description = "Numéro de l'épisode correspondant au fichier",
                            type = OptionType.INTEGER,
                            required = true,
                            minInt = 1
                    )
            }
    )
    public InteractionResponse executeFile(DiscordUser user, @Param("anime") long animeId, @Param("file") String file, @Param("episode") long episodeNumber) {

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

    @Slash(
            name = "task/import/directory",
            description = "\uD83D\uDD12 — Importe un fichier media en tant qu'épisode d'un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel le fichier sera importé",
                            type = OptionType.INTEGER,
                            required = true,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "directory",
                            description = "Dossier à importer",
                            type = OptionType.STRING,
                            required = true,
                            completion = @Completion(named = ImportableDirectoryCompletion.NAME)
                    ),
            }
    )
    public InteractionResponse executeDirectory(DiscordUser user, @Param("anime") long animeId, @Param("directory") String directory) throws IOException {

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


}
