package fr.anisekai.web.api;

import fr.anisekai.scheduler.tasking.data.TaskMeta;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.repositories.WorkerRepository;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.WorkerService;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.dto.TaskCompletionRequest;
import fr.anisekai.web.dto.TaskSummary;
import fr.anisekai.web.dto.WorkerPingRequest;
import fr.anisekai.web.dto.WorkerPingResponse;
import fr.anisekai.web.enums.TokenScope;
import fr.anisekai.web.enums.TokenType;
import fr.anisekai.web.exceptions.WebException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/workers")
public class WorkerController {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkerController.class);

    private final WorkerRepository workerRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final WorkerService workerService;

    public WorkerController(WorkerRepository workerRepository, TaskRepository taskRepository, TaskService taskService, WorkerService workerService) {
        this.workerRepository = workerRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.workerService = workerService;
    }

    @PostMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION, scopes = TokenScope.WORKER)
    @Operation(summary = "Worker heartbeat and task assignment", description = "Heartbeat the single worker bound to the session token. Returns an available task or indicates no tasks are available. Rejected when the worker is already active.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Heartbeat recorded, task returned.",
                    content = @Content(schema = @Schema(implementation = WorkerPingResponse.class))),
            @ApiResponse(responseCode = "400", description = "No worker provisioned for this session token, or missing/unknown factory names.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
            @ApiResponse(responseCode = "409", description = "Worker is already active.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
    })
    @Transactional
    public ResponseEntity<WorkerPingResponse> ping(@RequestBody @Valid WorkerPingRequest request,
                                                  SessionToken session) {

        Worker worker = workerRepository.findForUpdateById(session.getId())
                .orElseThrow(() -> new WebException(HttpStatus.BAD_REQUEST,
                        "No worker provisioned for this session token. Reissue an API key with the worker scope."));

        if (!workerService.isStale(worker, Instant.now())) {
            LOGGER.warn("Worker {} ping rejected: worker already active", worker.getId());
            throw new WebException(HttpStatus.CONFLICT, "Worker is already active");
        }

        workerService.heartbeat(worker, request.workerName());

        Task task = taskService.pollForWorker(worker, request.factoryNames()).orElse(null);

        WorkerPingResponse response = new WorkerPingResponse(worker.getId(),
                task != null ? new TaskSummary(task.getId(), task.getFactoryName(), task.getName(),
                        task.getStatus(), task.getPriority(), task.getActiveKey(), task.getStartedAt(), task.getCompletedAt(),
                        task.getArguments())
                        : null,
                task != null,
                task != null ? task.getIsolationId() : null);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{taskId}/success", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION, scopes = TokenScope.WORKER)
    @Operation(summary = "Report task success", description = "Report that the worker successfully completed the task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task success reported."),
            @ApiResponse(responseCode = "404", description = "Task not found or not assigned to this worker.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
    })
    public ResponseEntity<?> reportSuccess(@PathVariable UUID taskId,
                                          @RequestBody TaskCompletionRequest request,
                                          SessionToken session) {

        Task task = requireAssignedTask(taskId, session);

        TaskMeta meta = TaskMeta.of(task);
        taskService.resolveSuccess(meta, request.result());

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{taskId}/failure", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION, scopes = TokenScope.WORKER)
    @Operation(summary = "Report task failure", description = "Report that the worker failed to complete the task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task failure reported."),
            @ApiResponse(responseCode = "404", description = "Task not found or not assigned to this worker.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
    })
    public ResponseEntity<?> reportFailure(@PathVariable UUID taskId,
                                          @RequestBody TaskCompletionRequest request,
                                          SessionToken session) {

        Task task = requireAssignedTask(taskId, session);

        TaskMeta meta = TaskMeta.of(task);
        taskService.resolveFailure(meta, new RuntimeException(request.errorMessage()));

        return ResponseEntity.ok().build();
    }

    private Task requireAssignedTask(UUID taskId, SessionToken session) {
        Worker worker = workerRepository.findById(session.getId())
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND,
                        "Task not found or not assigned to this worker"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND,
                        "Task not found or not assigned to this worker"));

        Worker assigned = task.getAssignedWorker();
        if (assigned == null || !assigned.getId().equals(worker.getId())) {
            LOGGER.warn("Worker {} attempted to report task {} owned by {}", worker.getId(), task.getId(),
                    assigned == null ? "nobody" : assigned.getId());
            throw new WebException(HttpStatus.NOT_FOUND,
                    "Task not found or not assigned to this worker");
        }

        return task;
    }
}
