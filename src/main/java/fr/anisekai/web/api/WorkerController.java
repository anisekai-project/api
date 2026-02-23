package fr.anisekai.web.api;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.domain.enums.TaskStatus;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.WorkerService;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.dto.worker.WorkerRegistrationRequest;
import fr.anisekai.web.dto.worker.WorkerRegistrationResponse;
import fr.anisekai.web.enums.TokenScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/worker")
@Tag(name = "Workers", description = "Endpoints for distributed task workers.")
public class WorkerController {

    private final WorkerService workerService;
    private final TaskService   taskService;

    public WorkerController(WorkerService workerService, TaskService taskService) {

        this.workerService = workerService;
        this.taskService   = taskService;
    }

    @PostMapping("/register")
    @RequireAuth(scopes = {TokenScope.WORKER})
    @Operation(summary = "Register a new worker instance")
    public ResponseEntity<WorkerRegistrationResponse> registerWorker(SessionToken session, @RequestBody(required = false) WorkerRegistrationRequest request) {

        String hostname = (request != null) ? request.hostname() : null;
        Worker worker   = this.workerService.registerWorker(session, hostname);
        return ResponseEntity.ok(new WorkerRegistrationResponse(worker.getId()));
    }

    @PostMapping("/{workerId}/heartbeat")
    @RequireAuth(scopes = {TokenScope.WORKER})
    @Operation(summary = "Send a heartbeat", description = "Allows a worker to signal it is still alive and processing its task, extending the task's expiration lease.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Heartbeat acknowledged, but no task lease available to extend."),
            @ApiResponse(responseCode = "200", description = "Task lease of the current worker has been extended.")
    })
    public ResponseEntity<Void> doWorkerHeartbeat(SessionToken session, @PathVariable UUID workerId) {

        Worker worker = this.workerService.requireWorkerOwnership(workerId, session);
        worker.setLastHeartbeat(Instant.now());
        worker = this.workerService.getRepository().save(worker);

        Optional<Task> optionalTask = this.taskService
                .getRepository()
                .findByWorkerAndStatus(worker, TaskStatus.EXECUTING);

        if (optionalTask.isPresent()) {
            this.taskService.extendTaskLease(optionalTask.get());
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.noContent().build();
        }
    }

}
