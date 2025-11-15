package fr.anisekai.web.dto.worker;

import io.swagger.v3.oas.annotations.media.Schema;

public record WorkerRegistrationRequest(
        @Schema(
                description = "An optional, human-readable name for the worker, used for observability.",
                example = "desktop-ryzen9"
        ) String hostname
) {

}
