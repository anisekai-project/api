package fr.anisekai.web.api.requests.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;

@Schema(description = "Structure of an authentication request.")
public record AuthenticationRequest(
        @Schema(
                description = "The auth code given by discord.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "ChqHbhWsOZ1Et9yTCJR8v73a6uHFUo"
        ) @NotBlank @NotNull String code
) {

}
