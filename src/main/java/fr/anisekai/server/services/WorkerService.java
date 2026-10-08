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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkerService extends AnisekaiService<Worker, UUID, WorkerRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerService.class);

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
     * Claim or renew a worker lease. A different claimant may take over only after the
     * current lease is stale, at which point its executing tasks are released first.
     *
     * @param worker
     *         The locked worker row.
     * @param instanceId
     *         The claimant's process identifier.
     * @param name
     *         The worker-declared name, may be {@code null} to keep the current one.
     *
     * @return Whether the lease was claimed or renewed.
     */
    @Transactional
    public boolean claim(@NotNull Worker worker, @NotNull UUID instanceId, @Nullable String name) {

        UUID currentInstanceId = worker.getInstanceId();
        if (currentInstanceId != null && !currentInstanceId.equals(instanceId)) {
            if (!this.isStale(worker, Instant.now())) {
                return false;
            }
            this.release(worker);
        }

        worker.setInstanceId(instanceId);
        this.heartbeat(worker, name);
        return true;
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
                UUID isolationId = task.getIsolationId();
                task.setIsolationId(null);
                this.discardIsolationAfterCommit(isolationId);
            }
            task.setStatus(TaskStatus.SCHEDULED);
            task.setStartedAt(null);
            task.setAssignedWorker(null);
        }
        return this.taskRepository.saveAll(tasks);
    }

    private void discardIsolationAfterCommit(UUID isolationId) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            this.library.discardIsolation(isolationId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {

                try {
                    library.discardIsolation(isolationId);
                } catch (RuntimeException e) {
                    LOGGER.error("Failed to discard released isolation {}", isolationId, e);
                }
            }
        });
    }
}
