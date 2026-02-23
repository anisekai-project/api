package fr.anisekai.web.api.responses.entities;

import fr.anisekai.server.domain.entities.Episode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

@Schema(description = "Structure of an episode response.")
public record EpisodeResponse(
        @Schema(
                description = "The episode identifier.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "123"
        ) long id,

        @Schema(
                description = "The episode number within the anime.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "1"
        ) long number
) {

    public static @NotNull EpisodeResponse of(@NotNull Episode episode) {

        return new EpisodeResponse(
                episode.getId(),
                episode.getNumber()
        );
    }

}
