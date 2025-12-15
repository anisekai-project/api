package fr.anisekai.discord.completions;

import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.providers.interactions.slash.interfaces.CompletionProvider;
import fr.anisekai.discord.annotations.CompletionBean;
import fr.anisekai.library.Library;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

import java.util.stream.Stream;

@CompletionBean(ImportableDirectoryCompletion.NAME)
public class ImportableDirectoryCompletion implements CompletionProvider {

    public static final String NAME = "completion:importable.directory";

    private final Library library;

    public ImportableDirectoryCompletion(Library library) {

        this.library = library;
    }

    @Override
    public Stream<Command.Choice> complete(Request<CommandAutoCompleteInteraction> request) {

        return this.library.getImportableDirectories()
                           .stream()
                           .map(directory -> new Command.Choice(directory, directory));
    }

}
