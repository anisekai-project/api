package fr.anisekai.discord.tasks.watchlist.update;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.responses.embeds.WatchlistEmbed;
import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.domain.entities.Watchlist;
import fr.anisekai.server.exceptions.task.FatalTaskException;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.services.WatchlistService;
import fr.anisekai.utils.DiscordUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class WatchlistUpdateTask implements TaskHandler<WatchlistUpdateTaskInput, Nothing> {

    private final WatchlistService service;
    private final AnimeService     animeService;
    private final InterestService  interestService;
    private final JDAStore         store;

    public WatchlistUpdateTask(WatchlistService service, AnimeService animeService, InterestService interestService, JDAStore store) {

        this.service         = service;
        this.animeService    = animeService;
        this.interestService = interestService;
        this.store           = store;
    }

    @Override
    public @NonNull Nothing handle(@NonNull WatchlistUpdateTaskInput arguments) throws Exception {

        MessageChannel channel   = this.store.requireWatchlistChannel();
        Watchlist      watchlist = this.service.requireById(arguments.list());

        Message message = DiscordUtils
                .findExistingMessage(channel, watchlist)
                .orElseThrow(() -> new FatalTaskException("Watchlist has most likely not been created yet."));

        List<Anime>    animes    = this.animeService.getOfStatus(watchlist.getId());
        List<Interest> interests = this.interestService.getInterests(animes);

        WatchlistEmbed embed = new WatchlistEmbed();
        embed.setWatchlistContent(watchlist.getId(), animes, interests);

        MessageEditBuilder meb = new MessageEditBuilder();
        meb.setEmbeds(embed.build());
        message.editMessage(meb.build()).complete();

        return Nothing.INSTANCE;
    }

}
