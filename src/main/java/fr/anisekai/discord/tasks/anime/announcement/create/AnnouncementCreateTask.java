package fr.anisekai.discord.tasks.anime.announcement.create;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.responses.messages.AnimeCardMessage;
import fr.anisekai.discord.tasks.anime.announcement.AnnouncementTask;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.exceptions.task.FatalTaskException;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.utils.DiscordUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.List;

public class AnnouncementCreateTask extends AnnouncementTask {

    public AnnouncementCreateTask(AnimeService animeService, InterestService interestService, JDAStore store) {

        super(animeService, interestService, store);
    }

    @Override
    public void doAnnouncementStuff(TextChannel channel, Role role, Anime anime, List<Interest> interests, AnimeCardMessage card) {

        if (DiscordUtils.findExistingMessage(channel, anime).isPresent()) {
            throw new FatalTaskException("The announcement message cannot be created as it already exists.");
        }

        MessageCreateBuilder builder = new MessageCreateBuilder();
        card.getHandler().accept(builder);
        Message message = channel.sendMessage(builder.build()).complete();
        this.getAnimeService().mod(anime.getId(), entity -> entity.setAnnouncementId(message.getIdLong()));
    }

}
