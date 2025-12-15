package fr.anisekai.discord.completions;

import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.providers.interactions.slash.interfaces.CompletionProvider;
import fr.anisekai.discord.annotations.CompletionBean;
import fr.anisekai.server.repositories.AnimeRepository;
import fr.anisekai.utils.StringUtils;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

import java.util.stream.Stream;

@CompletionBean(AnimeCompletion.NAME)
public class AnimeCompletion implements CompletionProvider {

    public static final String NAME = "completion:animes";

    private final AnimeRepository repository;

    public AnimeCompletion(AnimeRepository repository) {

        this.repository = repository;
    }

    @Override
    public boolean isFiltered() {

        return true;
    }

    @Override
    public Stream<Command.Choice> complete(Request<CommandAutoCompleteInteraction> request) {

        String value = request.getEvent().getFocusedOption().getValue();

        return this.repository.findAll()
                              .stream()
                              .sorted()
                              .filter(anime -> anime.getTitle().toLowerCase().contains(value.toLowerCase()))
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
                              ));
    }

}
