package fr.anisekai.discord.interactions.anime;

import fr.alexpado.interactions.annotations.Completion;
import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.Texts;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.completions.AnimeCompletion;
import fr.anisekai.discord.completions.WatchlistCompletion;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.services.AnimeService;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.List;

@DiscordBean
@RequireAdmin
public class AnimeStatusSlashInteraction {

    private final AnimeService service;

    public AnimeStatusSlashInteraction(AnimeService service) {

        this.service = service;
    }

    @Slash(
            name = "anime/list",
            description = "\uD83D\uDD12 — Change à quelle watchlist appartient un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel la fiche sera envoyée.",
                            required = true,
                            type = OptionType.INTEGER,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "watchlist",
                            description = "Nouvelle watchlist pour l'anime",
                            type = OptionType.STRING,
                            required = true,
                            completion = @Completion(named = WatchlistCompletion.NAME)
                    )
            }
    )
    public InteractionResponse execute(@Param("anime") long animeId, @Param("watchlist") AnimeList status) {

        Anime anime = this.service.mod(animeId, entity -> entity.setList(status));
        return DiscordResponse.info(
                "La watchlist de l'anime **%s** a bien été changée.\n%s",
                anime.getTitle(),
                Texts.formatted(anime.getList())
        );
    }


    @Slash(
            name = "anime/bulk-list",
            description = "\uD83D\uDD12 — Déplace tous les animes d'une liste à une autre.",
            options = {
                    @Option(
                            name = "source",
                            description = "Liste source",
                            type = OptionType.STRING,
                            required = true,
                            completion = @Completion(named = WatchlistCompletion.NAME)
                    ),
                    @Option(
                            name = "destination",
                            description = "Liste de destination",
                            type = OptionType.STRING,
                            required = true,
                            completion = @Completion(named = WatchlistCompletion.NAME)
                    )
            }
    )
    public InteractionResponse execute(@Param("source") AnimeList source, @Param("destination") AnimeList destination) {

        List<Anime> animes = this.service.move(source, destination);

        return DiscordResponse.info(
                "La watchlist de **%s** anime(s) a bien été changée.\n%s **->** %s",
                animes.size(),
                Texts.formatted(source),
                Texts.formatted(destination)
        );
    }


}
