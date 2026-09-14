package fr.anisekai.web.dto;

import java.util.List;
import java.util.UUID;

public record WorkerPingRequest(
        UUID workerId,
        List<String> factoryNames,
        String workerName
) {
}
