package fr.anisekai.web.dto;

import java.util.UUID;

public record TaskCompletionRequest(
        UUID taskId,
        String result,
        String errorMessage
) {
}