package fr.anisekai.discord.tasks.watchlist.create;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.responses.embeds.WatchlistEmbed;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.entities.Watchlist;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.WatchlistService;
import fr.anisekai.server.tasking.TaskExecutor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.List;

public class WatchlistCreateTask implements TaskExecutor {

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
    public void execute(ITimedAction timer, AnisekaiJson params) {

        MessageChannel channel = this.store.requireWatchlistChannel();

        timer.action("reset", "Reset the watchlists");
        List<Watchlist> watchlists = this.service.reset();
        timer.endAction();

        timer.action("send", "Send the watchlists on Discord");
        for (Watchlist watchlist : watchlists) {
            timer.action(watchlist.getId().name(), "Handle watchlist");

            timer.action("load", "Load the watchlist data");
            List<Anime>    animes    = this.animeService.getOfStatus(watchlist.getId());
            List<Interest> interests = this.interestService.getInterests(animes);
            timer.endAction();

            timer.action("embed", "Creating embed");
            WatchlistEmbed embed = new WatchlistEmbed();
            embed.setWatchlistContent(watchlist.getId(), animes, interests);
            timer.endAction();

            timer.action("update", "Sending the message");
            MessageCreateBuilder mcb = new MessageCreateBuilder();
            mcb.setEmbeds(embed.build());
            Message message = channel.sendMessage(mcb.build()).complete();
            timer.endAction();

            timer.action("save", "Saving the message ID");
            this.service.mod(watchlist.getId(), entity -> entity.setMessageId(message.getIdLong()));
            timer.endAction();
        }
        timer.endAction();

    }

}
