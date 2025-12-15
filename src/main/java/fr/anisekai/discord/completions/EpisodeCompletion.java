package fr.anisekai.discord.completions;

import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.providers.interactions.slash.interfaces.CompletionProvider;
import fr.anisekai.discord.annotations.CompletionBean;
import fr.anisekai.server.repositories.EpisodeRepository;
import fr.anisekai.utils.StringUtils;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

import java.util.stream.Stream;

@CompletionBean(EpisodeCompletion.NAME)
public class EpisodeCompletion implements CompletionProvider {

    public static final String NAME = "completion:episode";

    private final EpisodeRepository repository;

    public EpisodeCompletion(EpisodeRepository repository) {

        this.repository = repository;
    }

    @Override
    public boolean isFiltered() {

        return true;
    }

    @Override
    public Stream<Command.Choice> complete(Request<CommandAutoCompleteInteraction> request) {

        String value = request.getEvent().getFocusedOption().getValue();

        return this.repository
                .findAllByReadyTrue()
                .stream()
                .filter(episode -> episode.getAnime().getTitle().toLowerCase().contains(value.toLowerCase()))
                .sorted()
                .map(episode -> new Command.Choice(
                        StringUtils.truncate(
                                String.format(
                                        "%s %s - Épisode %02d",
                                        episode.getAnime().getList().getIcon(),
                                        episode.getAnime().getTitle(),
                                        episode.getNumber()
                                ),
                                100, 30
                        ), episode.getId()
                ));
    }

}
