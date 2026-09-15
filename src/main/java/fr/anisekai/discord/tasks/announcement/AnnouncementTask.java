package fr.anisekai.discord.tasks.announcement;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.messages.AnimeCardMessage;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskHandler;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Interest;
import fr.anisekai.server.exceptions.task.FatalTaskException;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.InterestService;
import fr.anisekai.utils.DiscordUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public class AnnouncementTask implements TaskHandler<AnnouncementTaskInput, AnnouncementTaskOutput> {

    private final JDAStore        store;
    private final AnimeService    animeService;
    private final InterestService interestService;

    public AnnouncementTask(JDAStore store, AnimeService animeService, InterestService interestService) {

        this.store           = store;
        this.animeService    = animeService;
        this.interestService = interestService;
    }

    @Override
    public @NonNull AnnouncementTaskOutput handle(@NonNull AnnouncementTaskInput arguments) {

        TextChannel         channel         = this.store.requireAnnouncementChannel();
        Role                role            = this.store.requireAnnouncementRole();
        Anime               anime           = this.animeService.requireById(arguments.animeId());
        List<Interest>      interests       = this.interestService.getInterests(anime);
        InteractionResponse response        = new AnimeCardMessage(anime, interests, role);
        Optional<Message>   optionalMessage = DiscordUtils.findExistingMessage(channel, anime);

        if (arguments.edit()) {
            if (optionalMessage.isEmpty()) {
                throw new FatalTaskException("Cannot update announcement message as it does not exist.");
            }
            return new AnnouncementTaskOutput(this.updateAnimeAnnouncement(optionalMessage.get(), response));
        }

        if (optionalMessage.isPresent()) {
            throw new FatalTaskException("The announcement message cannot be created as it already exists.");
        }

        return new AnnouncementTaskOutput(this.createAnimeAnnouncement(channel, response));
    }

    private long createAnimeAnnouncement(MessageChannel channel, InteractionResponse response) {

        MessageCreateBuilder builder = new MessageCreateBuilder();
        response.getHandler().accept(builder);
        return channel.sendMessage(builder.build()).complete().getIdLong();
    }

    private long updateAnimeAnnouncement(Message message, InteractionResponse response) {

        MessageEditBuilder builder = new MessageEditBuilder();
        response.getHandler().accept(builder);
        return message.editMessage(builder.build()).complete().getIdLong();
    }

}
