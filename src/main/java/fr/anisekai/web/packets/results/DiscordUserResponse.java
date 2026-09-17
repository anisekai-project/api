package fr.anisekai.web.packets.results;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record DiscordUserResponse(
        @Schema(description = "The Discord user ID.")
        long id,
        @Schema(description = "The username.")
        String username,
        @JsonProperty("discriminator")
        @Schema(description = "The discriminator (nullable for newer accounts).")
        String discriminator,
        @JsonProperty("global_name")
        @Schema(description = "The global name.")
        String globalName,
        @Schema(description = "The avatar hash.")
        String avatar
) {

}
