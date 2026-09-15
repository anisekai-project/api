package fr.anisekai.discord.interactions.task;

import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.library.tasks.torrent.sourcing.TorrentSourcingTaskFactory;
import fr.anisekai.library.tasks.torrent.sourcing.TorrentSourcingTaskInput;
import fr.anisekai.server.domain.entities.Task;
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
    public InteractionResponse execute(@Param("url") String url) {

        Optional<String> optionalUserSource    = Optional.ofNullable(url);
        Optional<String> optionalDefaultSource = this.settingService.getDownloadSource();


        if (optionalUserSource.isPresent()) {
            this.queueSourcing(optionalUserSource.get());
            return DiscordResponse.success("La vérification va être effectuée sous peu.");
        }

        if (optionalDefaultSource.isPresent()) {
            this.queueSourcing(optionalDefaultSource.get());
            return DiscordResponse.success("La vérification va être effectuée sous peu.");
        }

        return DiscordResponse.error("Aucune source disponible pour le téléchargement automatique.");
    }

    private void queueSourcing(String url) {

        this.service.queueOne(
                TorrentSourcingTaskFactory.class,
                new TorrentSourcingTaskInput(url, Task.PRIORITY_MANUAL_HIGH),
                Task.PRIORITY_MANUAL_HIGH
        );
    }

}
