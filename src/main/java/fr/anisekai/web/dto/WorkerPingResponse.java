package fr.anisekai.web.dto;

import java.util.UUID;

public record WorkerPingResponse(
        UUID workerId,
        TaskSummary task,
        boolean hasTask
) {
}