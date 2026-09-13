package fr.anisekai.discord.tasks.watchlist.create;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.responses.embeds.WatchlistEmbed;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.entities.Watchlist;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.WatchlistService;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchlistCreateTask implements TaskHandler<Nothing, WatchlistCreateTaskOutput> {

    private final JDAStore         store;
    private final WatchlistService service;
    private final AnimeService     animeService;
    private final InterestService  interestService;

    public WatchlistCreateTask(JDAStore store, WatchlistService service, AnimeService animeService, InterestService interestService) {

        this.store           = store;
        this.service         = service;
        this.animeService    = animeService;
        this.interestService = interestService;
    }

    @Override
    public @NonNull WatchlistCreateTaskOutput handle(@NonNull Nothing arguments) {

        MessageChannel  channel    = this.store.requireWatchlistChannel();
        List<Watchlist> watchlists = this.service.reset();

        Map<AnimeList, Long> listMessageMap = new HashMap<>();

        for (Watchlist watchlist : watchlists) {

            List<Anime>    animes    = this.animeService.getOfStatus(watchlist.getId());
            List<Interest> interests = this.interestService.getInterests(animes);

            WatchlistEmbed embed = new WatchlistEmbed();
            embed.setWatchlistContent(watchlist.getId(), animes, interests);

            MessageCreateBuilder mcb = new MessageCreateBuilder();
            mcb.setEmbeds(embed.build());
            Message message = channel.sendMessage(mcb.build()).complete();

            listMessageMap.put(watchlist.getId(), message.getIdLong());
        }

        return new WatchlistCreateTaskOutput(listMessageMap);
    }

}
