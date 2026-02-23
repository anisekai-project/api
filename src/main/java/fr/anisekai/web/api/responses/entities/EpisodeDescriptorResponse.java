package fr.anisekai.web.api.responses.entities;

import fr.anisekai.server.domain.entities.Episode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@Schema(description = "Structure of an episode descriptor response.")
public record EpisodeDescriptorResponse(
        @Schema(
                description = "The episode identifier.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "123"
        ) long id,

        @Schema(
                description = "The episode number within the anime.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "1"
        ) long number,

        @Schema(
                description = "The anime's title to which the episode belongs to.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Arifureta Shokugyou de Sekai Saikyou : Maboroshi no Bouken to Kiseki no Kaikou"
        ) @NotNull String anime,

        @Schema(
                description = "The episode track list.",
                requiredMode = Schema.RequiredMode.REQUIRED
        ) @NotNull Collection<TrackResponse> tracks
) {

    public static @NotNull EpisodeDescriptorResponse of(@NotNull Episode episode) {

        return new EpisodeDescriptorResponse(
                episode.getId(),
                episode.getNumber(),
                episode.getAnime().getTitle(),
                episode.getTracks().stream().map(TrackResponse::of).toList()
        );
    }

}
