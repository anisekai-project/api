package fr.anisekai.web.api;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.repositories.WorkerRepository;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.scheduler.tasking.data.TaskMeta;
import fr.anisekai.web.dto.TaskCompletionRequest;
import fr.anisekai.web.dto.TaskSummary;
import fr.anisekai.web.dto.WorkerPingRequest;
import fr.anisekai.web.dto.WorkerPingResponse;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.web.enums.TokenType;
import static fr.anisekai.web.enums.TokenScope.WORKER;

@RestController
@RequestMapping("/api/v3/workers")
public class WorkerController {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkerController.class);

    private final WorkerRepository workerRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;

    public WorkerController(WorkerRepository workerRepository, TaskRepository taskRepository, TaskService taskService) {
        this.workerRepository = workerRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }

    @PostMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION, scopes = TokenScope.WORKER)
    @Operation(summary = "Worker ping for task assignment", description = "Register or update a worker instance. Returns an available task or indicates no tasks are available.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worker registered, task returned.",
                    content = @Content(schema = @Schema(implementation = WorkerPingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid worker UUID provided.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
    })
    public ResponseEntity<WorkerPingResponse> ping(@RequestBody @Valid WorkerPingRequest request,
                                                 SessionToken session) {

        UUID sessionUuid = session.getId();

        UUID workerUuid;
        if (request.workerId() != null) {
            Optional<Worker> existing = workerRepository.findByIdAndSessionTokenId(request.workerId(), sessionUuid);
            if (existing.isPresent()) {
                workerUuid = existing.get().getId();
            } else {
                throw new WebException(HttpStatus.BAD_REQUEST,
                        "Worker UUID not found for this session token");
            }
        } else {
            workerUuid = UUID.randomUUID();
        }

        Worker worker = workerRepository.findById(workerUuid)
                .orElseGet(() -> {
                    Worker w = new Worker();
                    w.setId(workerUuid);
                    w.setSessionTokenId(sessionUuid);
                    w.setLastPing(Instant.now());
                    return w;
                });

        worker.setLastPing(Instant.now());
        workerRepository.save(worker);

        String factoryName = request.factoryName();
        Task task = null;
        if (factoryName != null) {
            task = taskRepository.findFirstByFactoryNameAndStatusIn(factoryName,
                    Arrays.asList(TaskStatus.SCHEDULED, TaskStatus.EXECUTING));
        } else {
            task = taskRepository.findFirstByStatusIn(Arrays.asList(TaskStatus.SCHEDULED, TaskStatus.EXECUTING));
        }

        if (task != null) {
            task.setStatus(TaskStatus.EXECUTING);
            task.setStartedAt(Instant.now());
            taskRepository.save(task);
        }

        WorkerPingResponse response = new WorkerPingResponse(workerUuid,
                task != null ? new TaskSummary(task.getId(), task.getFactoryName(), task.getName,
                        task.getStatus(), task.getPriority(), task.getActiveKey(), task.getStartedAt(), task.getCompletedAt())
                        : null,
                task != null);

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

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND,
                        "Task not found"));

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

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND,
                        "Task not found"));

        TaskMeta meta = TaskMeta.of(task);
        taskService.resolveFailure(meta, new RuntimeException(request.errorMessage()));

        return ResponseEntity.ok().build();
    }
}