package fr.anisekai.web.packets.results;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record DiscordTokenResponse(
        @JsonProperty("access_token")
        @Schema(description = "The access token.")
        String accessToken,
        @JsonProperty("token_type")
        @Schema(description = "The token type.")
        String tokenType,
        @JsonProperty("expires_in")
        @Schema(description = "The token expiration in seconds.")
        int expiresIn,
        @JsonProperty("refresh_token")
        @Schema(description = "The refresh token.")
        String refreshToken,
        @JsonProperty("scope")
        @Schema(description = "The granted scopes.")
        String scope
) {

}
