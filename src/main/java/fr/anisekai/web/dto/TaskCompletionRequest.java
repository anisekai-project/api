package fr.anisekai.web.dto;

import java.util.UUID;

public record TaskCompletionRequest(
        UUID taskId,
        UUID workerId,
        String result,
        String errorMessage
) {
}
