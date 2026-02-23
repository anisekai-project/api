package fr.anisekai.web.api.responses.entities;

import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.web.enums.AnimeStorageState;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Schema(description = "Structure of an anime response.")
public record AnimeResponse(
        @Schema(
                description = "The anime identifier.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "23"
        ) long id,

        @Schema(
                description = "The anime licence group.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Arifureta Shokugyou de Sekai Saikyou"
        ) @NotNull String group,

        @Schema(
                description = "The anime order within the licence group (watch order).",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "5"
        ) byte order,

        @Schema(
                description = "The anime title.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Arifureta Shokugyou de Sekai Saikyou : Maboroshi no Bouken to Kiseki no Kaikou"
        ) @NotNull String title,

        @Schema(
                description = "The anime external link.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "https://www.nautiljon.com/animes/arifureta+shokugyou+de+sekai+saikyou+-+maboroshi+no+bouken+to+kiseki+no+kaikou.html"
        ) @NotNull String url,

        @Schema(
                description = "The anime state.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "COMPLETE"
        ) @NotNull AnimeStorageState state,

        @Schema(
                description = "The anime episode count.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "12"
        ) int episodeCount,

        @Schema(
                description = "The anime episode list.",
                requiredMode = Schema.RequiredMode.REQUIRED
        ) @NotNull List<EpisodeResponse> episodes
) {

    public static @NotNull AnimeResponse of(@NotNull Anime anime) {

        return new AnimeResponse(
                anime.getId(),
                anime.getGroup(),
                anime.getOrder(),
                anime.getTitle(),
                anime.getUrl(),
                AnimeStorageState.of(anime),
                Math.abs(anime.getTotal()),
                anime.getEpisodes().stream().map(EpisodeResponse::of).toList()
        );
    }

}
