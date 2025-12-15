package fr.anisekai.discord.interactions.task;

import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.interfaces.MessageRequestResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.library.tasks.factories.TorrentSourcingFactory;
import fr.anisekai.server.services.SettingService;
import fr.anisekai.server.services.TaskService;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.Optional;

@DiscordBean
@RequireAdmin
public class DownloadTaskSlashInteraction {

    private final TaskService    service;
    private final SettingService settingService;

    public DownloadTaskSlashInteraction(TaskService service, SettingService settingService) {

        this.service        = service;
        this.settingService = settingService;
    }

    @Slash(
            name = "task/download/check",
            description = "\uD83D\uDD12 — Vérifie si des épisodes peuvent être téléchargés.",
            options = @Option(
                    name = "url",
                    description = "Si précisé, utilisera cette URL au lieu de celle configurée par défaut.",
                    type = OptionType.STRING
            )
    )
    public MessageRequestResponse execute(@Param("url") String url) {

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

}
