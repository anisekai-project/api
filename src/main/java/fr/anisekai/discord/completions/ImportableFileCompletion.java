package fr.anisekai.discord.completions;

import fr.alexpado.interactions.interfaces.routing.Request;
import fr.alexpado.interactions.providers.interactions.slash.interfaces.CompletionProvider;
import fr.anisekai.discord.annotations.CompletionBean;
import fr.anisekai.library.Library;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

import java.util.stream.Stream;

@CompletionBean(ImportableFileCompletion.NAME)
public class ImportableFileCompletion implements CompletionProvider {

    public static final String NAME = "completion:importable.file";

    private final Library library;

    public ImportableFileCompletion(Library library) {

        this.library = library;
    }

    @Override
    public Stream<Command.Choice> complete(Request<CommandAutoCompleteInteraction> request) {

        return this.library.getImportableFiles()
                           .stream()
                           .map(directory -> new Command.Choice(directory, directory));
    }

}
