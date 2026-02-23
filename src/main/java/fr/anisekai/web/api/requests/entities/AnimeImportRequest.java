package fr.anisekai.web.api.requests.entities;

import fr.anisekai.server.domain.enums.AnimeList;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Deprecated
@Schema(description = "Structure of an anime import request.")
public record AnimeImportRequest(
        @Schema(description = "The group name into which the anime should be added.")
        @NotNull String group,
        @Schema(description = "The recommended watch order within the group.")
        byte order,
        @Schema(description = "The title, preferably in Romaji when possible.")
        @NotNull String title,
        @Schema(description = "The list (watchlist) into which the anime should be added.")
        @NotNull AnimeList list,
        @Schema(description = "The synopsis for this anime.")
        @NotNull String synopsis,
        @Schema(description = "All tags describing the anime.")
        @NotNull List<String> tags,
        @Schema(description = "Link to the thumbnail url of the anime.")
        @NotNull String image,
        @Schema(description = "Link to the page (Nautiljon) of the anime.")
        @NotNull String link,
        @Schema(description = "Total amount of episode in the anime. Can be negative to signal a 'previsional' amount of episode.")
        int total,
        @Schema(description = "Duration, in minutes, of one episode.")
        int episodeDuration
) {

}
