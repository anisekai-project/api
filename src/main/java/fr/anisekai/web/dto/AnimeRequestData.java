package fr.anisekai.web.dto;

import fr.anisekai.server.domain.enums.AnimeList;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AnimeRequestData(
        @Schema(description = "The group name into which the anime should be added.")
        String group,
        @Schema(description = "The recommended watch order within the group.")
        byte order,
        @Schema(description = "The title, preferably in Romaji when possible.")
        String title,
        @Schema(description = "The list (watchlist) into which the anime should be added.")
        AnimeList list,
        @Schema(description = "The synopsis for this anime.")
        String synopsis,
        @Schema(description = "All tags describing the anime.")
        List<String> tags,
        @Schema(description = "Link to the thumbnail url of the anime.")
        String image,
        @Schema(description = "Link to the page (Nautiljon) of the anime.")
        String link,
        @Schema(description = "Total amount of episode in the anime. Can be negative to signal a 'previsional' amount of episode.")
        int total,
        @Schema(description = "Duration, in minutes, of one episode.")
        int episodeDuration
) {

}
