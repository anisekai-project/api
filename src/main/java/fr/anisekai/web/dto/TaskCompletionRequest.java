package fr.anisekai.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TaskCompletionRequest(
        @NotNull UUID instanceId,
        String result,
        String errorMessage
) {
}
