package fr.anisekai.web.dto;

import java.util.UUID;

public record WorkerPingRequest(
        UUID workerId,
        String factoryName,
        String workerName
) {
}