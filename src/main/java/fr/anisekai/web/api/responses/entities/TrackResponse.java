package fr.anisekai.web.api.responses.entities;

import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.media.enums.Disposition;
import fr.anisekai.server.domain.entities.Track;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@Schema(description = "Structure of a track response.")
public record TrackResponse(
        @Schema(
                description = "The track identifier.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "123"
        ) long id,

        @Schema(
                description = "The track name",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "French"
        ) @NotNull String name,

        @Schema(
                description = "The track codec.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "AAC"
        ) @NotNull Codec codec,

        @Schema(
                description = "The track codec type.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "AUDIO"
        ) @NotNull CodecType type,

        @Schema(
                description = "The track language.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true,
                example = "jpn"
        ) @Nullable String language,

        @Schema(
                description = "The track disposition mode.",
                requiredMode = Schema.RequiredMode.REQUIRED
        ) @Nullable Collection<Disposition> dispositions
) {

    public static @NotNull TrackResponse of(@NotNull Track track) {

        return new TrackResponse(
                track.getId(),
                track.getName(),
                track.getCodec(),
                track.getCodec().getType(),
                track.getLanguage(),
                Disposition.fromBits(track.getDispositions())
        );
    }

}
