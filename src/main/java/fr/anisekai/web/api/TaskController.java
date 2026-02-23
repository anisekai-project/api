package fr.anisekai.web.api;

import fr.anisekai.sanctum.exceptions.context.ContextCommitException;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.domain.enums.TaskStatus;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.WorkerService;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.dto.worker.TaskAcquisitionResponse;
import fr.anisekai.web.dto.worker.TaskFailureRequest;
import fr.anisekai.web.dto.worker.tasks.MediaTaskDetails;
import fr.anisekai.web.dto.worker.tasks.TaskCompletionRequest;
import fr.anisekai.web.enums.TokenScope;
import fr.anisekai.web.enums.TokenType;
import fr.anisekai.web.exceptions.WebException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/tasks")
@Tag(name = "Tasks", description = "Endpoints for workers to acquire and manage tasks.")
public class TaskController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskController.class);

    private final TaskService   taskService;
    private final WorkerService workerService;

    public TaskController(TaskService taskService, WorkerService workerService) {

        this.taskService   = taskService;
        this.workerService = workerService;
    }

    private Task verifyTaskOwnership(SessionToken session, UUID workerId, Long taskId) {
        // First, verify the worker belongs to the session.
        this.workerService.requireWorkerOwnership(workerId, session);

        // Then, fetch the task and verify the worker owns it.
        Task   task   = this.taskService.requireById(taskId);
        Worker worker = task.getWorker();

        if (worker == null || !worker.getId().equals(workerId)) {
            throw new WebException(HttpStatus.FORBIDDEN, "Authenticated worker is not the owner of this task.");
        }

        if (task.getStatus() != TaskStatus.EXECUTING) {
            throw new WebException(
                    HttpStatus.CONFLICT,
                    "Task is not in EXECUTING state. Current status: " + task.getStatus()
            );
        }
        return task;
    }

    @GetMapping("/next")
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION, scopes = TokenScope.WORKER)
    @Operation(summary = "Request the next available task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A task was successfully acquired."),
            @ApiResponse(responseCode = "204", description = "No tasks are currently available.")
    })
    public ResponseEntity<TaskAcquisitionResponse> getNextTask(
            SessionToken session,
            @RequestHeader("X-Worker-ID") UUID workerId
    ) {

        Worker         worker       = this.workerService.requireWorkerOwnership(workerId, session);
        Optional<Task> optionalTask = this.taskService.acquirePublicTask(worker);

        if (optionalTask.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        Task             task    = optionalTask.get();
        MediaTaskDetails details = this.taskService.getMediaTaskDetails(task);
        return ResponseEntity.ok(TaskAcquisitionResponse.from(task, details));
    }

    @PostMapping("/{taskId}/complete")
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION, scopes = TokenScope.WORKER)
    public ResponseEntity<Void> completeTask(
            SessionToken session,
            @RequestHeader("X-Worker-ID") UUID workerId,
            @PathVariable Long taskId,
            @RequestBody TaskCompletionRequest request) {

        Task task = this.verifyTaskOwnership(session, workerId, taskId);

        try {
            this.taskService.completeTask(task, request.newTracks());
        } catch (ContextCommitException e) {
            this.taskService.failTask(task);
            throw new WebException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to commit isolation session. Task has been marked as failed.",
                    e
            );
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{taskId}/fail")
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION, scopes = TokenScope.WORKER)
    @Operation(summary = "Report task failure", description = "Called by a worker when it encounters an unrecoverable error during processing.")
    public ResponseEntity<Void> failTask(SessionToken session, @RequestHeader("X-Worker-ID") UUID workerId, @PathVariable Long taskId, @RequestBody(required = false) TaskFailureRequest request) {

        Task task = this.verifyTaskOwnership(session, workerId, taskId);

        String reason = (request != null) ? request.reason() : "No reason provided by worker.";
        this.taskService.failTask(task);

        // Optionally, you could log the reason here.
        LOGGER.warn("Worker {} reported failure for task {}: {}", workerId, taskId, reason);

        return ResponseEntity.ok().build();
    }

}
