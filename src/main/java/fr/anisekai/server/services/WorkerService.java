package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.library.Library;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.repositories.WorkerRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkerService extends AnisekaiService<Worker, UUID, WorkerRepository> {

    /**
     * Duration after which a worker that stopped pinging is considered stale and may be
     * replaced by a new claimant on its session token.
     */
    public static final Duration STALE_AFTER = Duration.ofSeconds(120);

    private final TaskRepository taskRepository;
    private final Library        library;

    public WorkerService(
            WorkerRepository repository,
            EntityEventProcessor eventProcessor,
            TaskRepository taskRepository,
            Library library
    ) {

        super(repository, eventProcessor);
        this.taskRepository = taskRepository;
        this.library        = library;
    }

    /**
     * Provision the single worker bound to the provided session token.
     * The worker shares the token identifier so a second worker can never exist for the token.
     *
     * @param token
     *         The session token the worker is bound to.
     *
     * @return The persisted worker.
     */
    @Transactional
    public Worker provision(@NotNull SessionToken token) {

        Worker worker = new Worker();
        worker.setId(token.getId());
        worker.setSessionToken(token);
        worker.setLastPing(Instant.now());
        return this.getRepository().save(worker);
    }

    public boolean isStale(@NotNull Worker worker, @NotNull Instant now) {

        return worker.getLastPing().isBefore(now.minus(STALE_AFTER));
    }

    /**
     * Record a heartbeat for the provided worker, optionally updating its display name.
     *
     * @param worker
     *         The worker pinging.
     * @param name
     *         The worker-declared name, may be {@code null} to keep the current one.
     *
     * @return The persisted worker.
     */
    @Transactional
    public Worker heartbeat(@NotNull Worker worker, @Nullable String name) {

        worker.setLastPing(Instant.now());
        if (name != null && !name.isBlank()) {
            worker.setName(name);
        }
        return this.getRepository().save(worker);
    }

    /**
     * Free every executing task assigned to the provided worker so they return to the schedule.
     *
     * @param worker
     *         The worker being released.
     *
     * @return The freed tasks.
     */
    @Transactional
    public List<Task> release(@NotNull Worker worker) {

        List<Task> tasks = this.taskRepository.findAllByAssignedWorkerAndStatus(worker, TaskStatus.EXECUTING);
        for (Task task : tasks) {
            if (task.getIsolationId() != null) {
                this.library.discardIsolation(task.getIsolationId());
                task.setIsolationId(null);
            }
            task.setStatus(TaskStatus.SCHEDULED);
            task.setStartedAt(null);
            task.setAssignedWorker(null);
        }
        return this.taskRepository.saveAll(tasks);
    }
}
