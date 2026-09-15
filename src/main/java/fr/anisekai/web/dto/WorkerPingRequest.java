package fr.anisekai.web.dto;

import java.util.List;

public record WorkerPingRequest(
        List<String> factoryNames,
        String workerName
) {
}
