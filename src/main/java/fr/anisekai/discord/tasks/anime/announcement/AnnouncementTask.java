package fr.anisekai.discord.tasks.anime.announcement;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.validation.JsonObjectRule;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.exceptions.tasks.UndefinedAnnouncementChannelException;
import fr.anisekai.discord.exceptions.tasks.UndefinedAnnouncementRoleException;
import fr.anisekai.discord.responses.messages.AnimeCardMessage;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.server.tasking.TaskExecutor;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.List;

public abstract class AnnouncementTask implements TaskExecutor {

    public static final String OPTION_ANIME = "anime";

    private final AnimeService    animeService;
    private final InterestService interestService;
    private final JDAStore        store;

    public AnnouncementTask(AnimeService animeService, InterestService interestService, JDAStore store) {

        this.animeService    = animeService;
        this.interestService = interestService;
        this.store           = store;
    }

    public AnimeService getAnimeService() {

        return this.animeService;
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) {

        TextChannel      channel   = this.getAnnouncementChannel();
        Role             role      = this.getAnnouncementRole();
        Anime            anime     = this.animeService.requireById(params.getLong(OPTION_ANIME));
        List<Interest>   interests = this.interestService.getInterests(anime);
        AnimeCardMessage card      = new AnimeCardMessage(anime, interests, role);

        this.doAnnouncementStuff(channel, role, anime, interests, card);
    }

    public abstract void doAnnouncementStuff(TextChannel channel, Role role, Anime anime, List<Interest> interests, AnimeCardMessage card);

    @Override
    public void validateParams(AnisekaiJson params) {

        params.validate(
                new JsonObjectRule(OPTION_ANIME, true, int.class, long.class, Integer.class, Long.class)
        );
    }

    private TextChannel getAnnouncementChannel() {

        return this.store.getAnnouncementChannel().orElseThrow(UndefinedAnnouncementChannelException::new);
    }

    private Role getAnnouncementRole() {

        return this.store.getAnnouncementRole().orElseThrow(UndefinedAnnouncementRoleException::new);
    }

}
