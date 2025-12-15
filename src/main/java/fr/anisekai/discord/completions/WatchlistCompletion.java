package fr.anisekai.discord.completions;


import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.providers.interactions.slash.interfaces.CompletionProvider;
import fr.anisekai.Texts;
import fr.anisekai.discord.annotations.CompletionBean;
import fr.anisekai.server.domain.enums.AnimeList;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

import java.util.stream.Stream;

@CompletionBean(WatchlistCompletion.NAME)
public class WatchlistCompletion implements CompletionProvider {

    public static final String NAME = "completion:watchlist";

    @Override
    public Stream<Command.Choice> complete(Request<CommandAutoCompleteInteraction> request) {

        return Stream.of(AnimeList.values())
                     .map(list -> new Command.Choice(Texts.formatted(list), list.name()));
    }

}
