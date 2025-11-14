package fr.anisekai.discord;

import fr.alexpado.jda.interactions.InteractionExtension;
import fr.alexpado.jda.interactions.entities.DispatchEvent;
import fr.alexpado.jda.interactions.interfaces.interactions.Injection;
import fr.alexpado.jda.interactions.interfaces.interactions.autocomplete.AutoCompleteProvider;
import fr.anisekai.Texts;
import fr.anisekai.library.Library;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.enums.BroadcastFrequency;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.UserService;
import fr.anisekai.utils.StringUtils;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class InteractionService {

    private final Library        library;
    private final UserService    userService;
    private final AnimeService   animeService;
    private final EpisodeService episodeService;

    public InteractionService(Library library, UserService userService, AnimeService animeService, EpisodeService episodeService) {

        this.library        = library;
        this.userService    = userService;
        this.animeService   = animeService;
        this.episodeService = episodeService;
    }

    public void using(InteractionExtension extension, Map<String, AutoCompleteProvider> completionMap) {

        extension.getSlashContainer().addClassMapping(DiscordUser.class, this.entityUserInterfaceMapper());
        extension.getSlashContainer().addClassMapping(DiscordUser.class, this.entityUserMapper());
        extension.getButtonContainer().addClassMapping(DiscordUser.class, this.entityUserInterfaceMapper());
        extension.getButtonContainer().addClassMapping(DiscordUser.class, this.entityUserMapper());

        completionMap.put("anime", this::animeCompletion);
        completionMap.put("watchlist", this::watchlistCompletion);
        completionMap.put("frequency", this::frequencyCompletion);
        completionMap.put("importable:directories", this::importableDirectories);
        completionMap.put("importable:files", this::importableFiles);
        completionMap.put("episode", this::episodeCompletion);
    }

    private <T extends Interaction> Injection<DispatchEvent<T>, DiscordUser> entityUserMapper() {

        return (event, option) -> () -> this.userService.of(event.interaction().getUser());
    }

    private <T extends Interaction> Injection<DispatchEvent<T>, DiscordUser> entityUserInterfaceMapper() {

        return (event, option) -> () -> this.userService.of(event.interaction().getUser());
    }

    private List<Command.Choice> animeCompletion(DispatchEvent<CommandAutoCompleteInteraction> event, String name, String completionName, String value) {

        return this.animeService
                .getRepository()
                .findAll()
                .stream()
                .filter(anime -> anime.getTitle().toLowerCase().contains(value.toLowerCase()))
                .sorted()
                .map(anime -> new Command.Choice(
                        StringUtils.truncate(
                                String.format(
                                        "%s %s",
                                        anime.getList().getIcon(),
                                        anime.getTitle()
                                ),
                                100, 30
                        ),
                        anime.getId()
                ))
                .toList();
    }

    private List<Command.Choice> watchlistCompletion(DispatchEvent<CommandAutoCompleteInteraction> event, String name, String completionName, String value) {

        return Stream.of(AnimeList.values())
                     .filter(list -> Texts.formatted(list).toLowerCase().contains(value.toLowerCase()))
                     .map(list -> new Command.Choice(Texts.formatted(list), list.name()))
                     .toList();
    }

    private List<Command.Choice> frequencyCompletion(DispatchEvent<CommandAutoCompleteInteraction> event, String name, String completionName, String value) {

        return Stream.of(BroadcastFrequency.values())
                     .filter(frequency -> frequency.getDisplayName().toLowerCase().contains(value.toLowerCase()))
                     .map(frequency -> new Command.Choice(frequency.getDisplayName(), frequency.name()))
                     .toList();
    }

    private List<Command.Choice> importableDirectories(DispatchEvent<CommandAutoCompleteInteraction> event, String name, String completionName, String value) {

        return this.library.getImportableDirectories()
                           .stream()
                           .map(directory -> new Command.Choice(directory, directory))
                           .toList();
    }

    private List<Command.Choice> importableFiles(DispatchEvent<CommandAutoCompleteInteraction> event, String name, String completionName, String value) {

        return this.library.getImportableFiles()
                           .stream()
                           .map(directory -> new Command.Choice(directory, directory))
                           .toList();
    }

    private List<Command.Choice> episodeCompletion(DispatchEvent<CommandAutoCompleteInteraction> event, String name, String completionName, String value) {

        return this.episodeService.getAllReady()
                                  .stream()
                                  .filter(episode -> episode.getAnime()
                                                            .getTitle()
                                                            .toLowerCase()
                                                            .contains(value.toLowerCase()))
                                  .sorted()
                                  .map(episode -> new Command.Choice(
                                          StringUtils.truncate(
                                                  String.format(
                                                          "%s %s - Épisode %d",
                                                          episode.getAnime().getList().getIcon(),
                                                          episode.getAnime().getTitle(),
                                                          episode.getNumber()
                                                  ),
                                                  100, 30
                                          ), episode.getId()
                                  )).toList();
    }

}
