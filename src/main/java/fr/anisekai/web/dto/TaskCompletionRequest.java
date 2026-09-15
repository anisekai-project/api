package fr.anisekai.web.dto;

public record TaskCompletionRequest(
        String result,
        String errorMessage
) {
}
