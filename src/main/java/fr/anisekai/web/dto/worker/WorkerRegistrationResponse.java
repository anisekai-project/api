package fr.anisekai.web.dto.worker;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record WorkerRegistrationResponse(
        @Schema(description = "The unique identifier assigned to this worker instance.")
        UUID workerId
) {

}
