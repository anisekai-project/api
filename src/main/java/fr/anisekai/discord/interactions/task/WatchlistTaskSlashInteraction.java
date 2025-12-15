package fr.anisekai.discord.interactions.task;

import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.interfaces.MessageRequestResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.discord.tasks.watchlist.create.WatchlistCreateFactory;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Watchlist;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.services.SettingService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.WatchlistService;

import java.util.Collection;
import java.util.List;

@DiscordBean
public class WatchlistTaskSlashInteraction {

    private final TaskService      service;
    private final SettingService   settingService;
    private final WatchlistService watchlistService;

    public WatchlistTaskSlashInteraction(TaskService service, SettingService settingService, WatchlistService watchlistService) {

        this.service          = service;
        this.settingService   = settingService;
        this.watchlistService = watchlistService;
    }

    @Slash(
            name = "task/watchlist/reset",
            description = "\uD83D\uDD12 — Créé les watchlist dans le salon configuré."
    )
    public MessageRequestResponse executeReset() {

        if (this.settingService.getWatchlistChannel().isEmpty()) {
            return DiscordResponse.error("Le salon des watchlist n'a pas été configuré.");
        }

        this.service.getFactory(WatchlistCreateFactory.class).queue(Task.PRIORITY_MANUAL_HIGH);
        return DiscordResponse.success(
                "Les listes ont été réinitialisée. Il vous faudra supprimer les messages des anciennes listes."
        );
    }

    @Slash(
            name = "task/watchlist/refresh",
            description = "\uD83D\uDD12 — Force l'actualisation des listes."
    )
    public MessageRequestResponse executeRefresh() {

        List<Watchlist>       lists    = this.watchlistService.getRepository().findAll();
        Collection<AnimeList> statuses = AnimeList.collect(AnimeList.Property.SHOW);

        if (lists.size() != statuses.size()) {
            return DiscordResponse.error(
                    "Le nombre de listes existantes ne correspond pas au nombre de listes à afficher."
            );
        }

        if (this.settingService.getWatchlistChannel().isEmpty()) {
            return DiscordResponse.error("Le salon des watchlist n'a pas été configuré.");
        }

        this.service.getFactory(WatchlistCreateFactory.class).queue(Task.PRIORITY_MANUAL_HIGH);
        return DiscordResponse.success(
                "Les listes ont été réinitialisée. Il vous faudra supprimer les messages des anciennes listes."
        );
    }

}
