package fr.anisekai.web.dto.worker;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskFailureRequest(
        @Schema(description = "An optional message from the worker explaining the reason for the failure.")
        String reason
) {

}
