package fr.anisekai.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ApiKeyRequest(
        @Schema(description = "Scopes granted to the API key. Unknown scopes are rejected.", example = "[\"anime.write\"]")
        List<String> scopes
) {

}
