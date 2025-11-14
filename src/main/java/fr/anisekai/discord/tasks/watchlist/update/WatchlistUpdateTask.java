package fr.anisekai.discord.tasks.watchlist.update;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.validation.JsonObjectRule;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.responses.embeds.WatchlistEmbed;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.entities.Watchlist;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.WatchlistService;
import fr.anisekai.server.tasking.TaskExecutor;
import fr.anisekai.utils.DiscordUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.util.List;

public class WatchlistUpdateTask implements TaskExecutor {

    public static final String OPTION_WATCHLIST = "watchlist";

    private final AnimeService     animeService;
    private final InterestService  interestService;
    private final WatchlistService watchlistService;
    private final JDAStore         store;

    public WatchlistUpdateTask(AnimeService animeService, InterestService interestService, WatchlistService watchlistService, JDAStore store) {

        this.animeService     = animeService;
        this.interestService  = interestService;
        this.watchlistService = watchlistService;
        this.store            = store;
    }

    @Override
    public void validateParams(AnisekaiJson params) {

        params.validate(
                new JsonObjectRule(OPTION_WATCHLIST, true, String.class)
        );
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) {

        timer.action("load", "Loading task data");
        String         rawStatus = params.getString(OPTION_WATCHLIST);
        AnimeList      status    = AnimeList.valueOf(rawStatus.toUpperCase());
        Watchlist      watchlist = this.watchlistService.requireById(status);
        MessageChannel channel   = this.store.requireWatchlistChannel();
        Message message = DiscordUtils
                .findExistingMessage(channel, watchlist)
                .orElseThrow(() -> new IllegalStateException("Watchlist has most likely not been created yet."));

        List<Anime>    animes    = this.animeService.getOfStatus(status);
        List<Interest> interests = this.interestService.getInterests(animes);
        timer.endAction();

        timer.action("embed", "Creating embed");
        WatchlistEmbed embed = new WatchlistEmbed();
        embed.setWatchlistContent(status, animes, interests);
        timer.endAction();

        timer.action("update", "Updating the message");
        MessageEditBuilder meb = new MessageEditBuilder();
        meb.setEmbeds(embed.build());
        message.editMessage(meb.build()).complete();
        timer.endAction();
    }

}
