package fr.anisekai.discord.tasks.anime.announcement.update;

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
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.util.List;
import java.util.Optional;

public class AnnouncementUpdateTask extends AnnouncementTask {

    public AnnouncementUpdateTask(AnimeService animeService, InterestService interestService, JDAStore store) {

        super(animeService, interestService, store);
    }

    @Override
    public void doAnnouncementStuff(TextChannel channel, Role role, Anime anime, List<Interest> interests, AnimeCardMessage card) {

        Optional<Message> optionalMessage = DiscordUtils.findExistingMessage(channel, anime);

        if (optionalMessage.isEmpty()) { // No message exists yet
            throw new FatalTaskException("Cannot update announcement message as it does not exist.");
        }

        Message            message = optionalMessage.get();
        MessageEditBuilder builder = new MessageEditBuilder();
        card.getHandler().accept(builder);
        message.editMessage(builder.build()).complete();
    }

}
