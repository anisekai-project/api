package fr.anisekai.web.dto;

import java.util.List;
import java.util.UUID;

public record WorkerPingRequest(
        List<String> factoryNames,
        String workerName,
        UUID currentTaskId
) {
}
