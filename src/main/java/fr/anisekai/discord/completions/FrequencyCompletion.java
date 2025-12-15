package fr.anisekai.discord.completions;

import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.providers.interactions.slash.interfaces.CompletionProvider;
import fr.anisekai.discord.annotations.CompletionBean;
import fr.anisekai.server.enums.BroadcastFrequency;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

import java.util.stream.Stream;

@CompletionBean(FrequencyCompletion.NAME)
public class FrequencyCompletion implements CompletionProvider {

    public static final String NAME = "completion:frequency";

    @Override
    public Stream<Command.Choice> complete(Request<CommandAutoCompleteInteraction> request) {

        return Stream.of(BroadcastFrequency.values())
                     .map(frequency -> new Command.Choice(frequency.getDisplayName(), frequency.name()));
    }

}
