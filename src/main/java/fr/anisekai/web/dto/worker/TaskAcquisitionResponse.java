package fr.anisekai.web.dto.worker;

import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.web.dto.worker.tasks.MediaTaskDetails;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record TaskAcquisitionResponse(
        @Schema(description = "The unique ID of the acquired task.")
        long taskId,
        @Schema(description = "The name of the factory that will execute the task.")
        String factory,
        @Schema(description = "The arguments required to execute the task.")
        Map<String, Object> arguments,
        @Schema(description = "The unique ID for the isolation context, if any.")
        UUID isolationId,
        @Schema(description = "Additional, type-specific details for the task, if applicable.")
        MediaTaskDetails details
) {

    public static TaskAcquisitionResponse from(Task task, MediaTaskDetails details) {

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("args", task.getArguments());
        arguments.put("details", details);

        return new TaskAcquisitionResponse(
                task.getId(),
                task.getFactoryName(),
                arguments,
                task.getIsolationId(),
                details
        );
    }

}
