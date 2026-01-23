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
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.services.AnimeService;
import net.dv8tion.jda.api.interactions.commands.OptionType;

@DiscordBean
@RequireAdmin
public class AnimeEpisodeSlashInteraction {

    private final AnimeService service;

    public AnimeEpisodeSlashInteraction(AnimeService service) {

        this.service = service;
    }

    @Slash(
            name = "anime/progress",
            description = "\uD83D\uDD12 — Défini le nombre d'épisode regardé pour un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel la fiche sera envoyée.",
                            required = true,
                            type = OptionType.INTEGER,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "progress",
                            description = "Nouvelle progression de visionnage pour l'anime",
                            type = OptionType.INTEGER,
                            required = true
                    )
            }
    )
    public InteractionResponse executeProgress(@Param("anime") long animeId, @Param("progress") long progress) {

        Anime anime = this.service.mod(animeId, entity -> entity.setWatched((int) progress));
        return DiscordResponse.info(
                "La progression de l'anime **%s** a bien été mis à jour.\n%s épisode(s) regardé(s)",
                anime.getTitle(),
                progress
        );
    }

    @Slash(
            name = "anime/total",
            description = "\uD83D\uDD12 — Défini le nombre d'épisode regardé pour un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel la fiche sera envoyée.",
                            required = true,
                            type = OptionType.INTEGER,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "total",
                            description = "Nombre total d'épisode pour l'anime",
                            type = OptionType.INTEGER,
                            required = true
                    )
            }
    )
    public InteractionResponse executeTotal(@Param("anime") long animeId, @Param("total") long total) {

        Anime anime = this.service.mod(animeId, entity -> entity.setTotal((int) total));
        return DiscordResponse.info(
                "Le nombre total d'épisode de l'anime **%s** a bien été mis à jour.\n%s épisode(s) au total %s",
                anime.getTitle(),
                total < 0 ? total * -1 : total,
                total < 0 ? "*(Estimation)*" : ""
        );
    }

    @Slash(
            name = "anime/duration",
            description = "\uD83D\uDD12 — Défini le temps de visionnage pour un épisode d'un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel la fiche sera envoyée.",
                            required = true,
                            type = OptionType.INTEGER,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "duration",
                            description = "Durée (en minute) pour un épisode",
                            type = OptionType.INTEGER,
                            required = true
                    )
            }
    )
    public InteractionResponse executeDuration(@Param("anime") long animeId, @Param("duration") long duration) {

        Anime anime = this.service.mod(animeId, entity -> entity.setEpisodeDuration((int) duration));
        return DiscordResponse.info(
                "La durée d'un épisode pour l'anime **%s** a bien été mis à jour.\n%s minutes",
                anime.getTitle(),
                duration
        );
    }

}
