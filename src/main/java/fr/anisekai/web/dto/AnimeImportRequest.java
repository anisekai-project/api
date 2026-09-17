package fr.anisekai.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AnimeImportRequest(
        @Schema(description = "The title of the anime.")
        String title,
        @Schema(description = "The list status of the anime.")
        String status,
        @Schema(description = "The link to the anime page.")
        String link,
        @Schema(description = "The URL of the anime thumbnail.")
        String image,
        @Schema(description = "The total amount of episodes.")
        int episode,
        @Schema(description = "The duration of one episode in minutes.")
        int time,
        @Schema(description = "The group name.")
        String group,
        @Schema(description = "The synopsis of the anime.")
        String synopsis,
        @Schema(description = "The genres of the anime.")
        List<String> genres,
        @Schema(description = "The themes of the anime.")
        List<String> themes,
        @Schema(description = "The recommended watch order.")
        byte order
) {

}
