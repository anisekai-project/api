package fr.anisekai.discord.interactions.anime;

import fr.alexpado.interactions.annotations.Completion;
import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.completions.AnimeCompletion;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.discord.responses.messages.AnimeCardMessage;
import fr.anisekai.discord.tasks.anime.announcement.create.AnnouncementCreateFactory;
import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.utils.FileUrlStreamer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.nio.file.Path;

@DiscordBean
public class AnimeViewSlashInteraction {

    private final Library      library;
    private final AnimeService service;
    private final TaskService  taskService;


    public AnimeViewSlashInteraction(Library library, AnimeService service, TaskService taskService) {

        this.library     = library;
        this.service     = service;
        this.taskService = taskService;
    }

    @Slash(
            name = "anime-card",
            description = "Permet de visionner la fiche d'un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel la fiche sera envoyée.",
                            required = true,
                            type = OptionType.INTEGER,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    )
            }
    )
    public InteractionResponse executeCard(@Param("anime") long animeId) {

        Anime anime = this.service.requireById(animeId);
        return new AnimeCardMessage(anime);
    }

    @Slash(
            name = "anime/announce",
            description = "\uD83D\uDD12 — Envoi un message d'annonce pour l'anime spécifié",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel l'annonce sera envoyée.",
                            required = true,
                            type = OptionType.INTEGER,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    )
            }
    )
    @RequireAdmin
    public InteractionResponse executeAnnouncement(@Param("anime") long animeId) {

        Anime anime = this.service.requireById(animeId);
        this.taskService.getFactory(AnnouncementCreateFactory.class).queue(anime, Task.PRIORITY_MANUAL_LOW);
        if (anime.getAnnouncementId() == null) {
            return DiscordResponse.info("L'annonce pour l'anime **%s** sera envoyée d'ici peu.", anime.getTitle());
        } else {
            return DiscordResponse.info("L'annonce pour l'anime **%s** sera mise à jour d'ici peu.", anime.getTitle());
        }
    }


    @Slash(
            name = "anime/event-image",
            description = "Permet de visionner la fiche d'un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "\uD83D\uDD12 — Change l'image d'évènement d'un anime.",
                            required = true,
                            type = OptionType.INTEGER,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "image",
                            description = "Image d'évènement pour l'anime",
                            type = OptionType.ATTACHMENT,
                            required = true
                    )
            }
    )
    @RequireAdmin
    public InteractionResponse execute(@Param("anime") long animeId, @Param("image") Message.Attachment attachment) throws Exception {


        if (!attachment.isImage() || !"webp".equals(attachment.getFileExtension())) {
            return DiscordResponse.error("Merci de fournir une image.\n***800x320 (webp)***");
        }

        if (attachment.getWidth() != 800 || attachment.getHeight() != 320) {
            return DiscordResponse.error(String.format(
                    "Les dimensions de l'image ne sont pas valide.\nReçu: %sx%s\nAttendu: 800x320",
                    attachment.getWidth(),
                    attachment.getHeight()
            ));
        }

        Anime anime = this.service.requireById(animeId);

        AccessScope scope = new AccessScope(Library.EVENT_IMAGES, anime);

        try (IsolationSession context = this.library.createIsolation(scope)) {
            Path destination = context.resolve(scope);

            if (!new FileUrlStreamer(destination, attachment.getUrl()).complete()) {
                throw new IllegalStateException("The file was not downloaded.");
            }
            context.commit();
        }

        return DiscordResponse.info("L'image a bien été mise à jour.").setImage(attachment.getProxyUrl());
    }

}
