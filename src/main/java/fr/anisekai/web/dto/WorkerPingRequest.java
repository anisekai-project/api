package fr.anisekai.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record WorkerPingRequest(
        @NotNull UUID instanceId,
        List<String> factoryNames,
        String workerName,
        UUID currentTaskId
) {
}
