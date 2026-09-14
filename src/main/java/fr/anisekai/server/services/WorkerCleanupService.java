package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.repositories.WorkerRepository;
import fr.anisekai.server.services.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

import static fr.anisekai.scheduler.tasking.enums.TaskStatus.*;

/**
 * Service that periodically cleans up workers that have not pinged the API within the threshold.
 * Stale workers' tasks are freed (reset to SCHEDULED) so they can be picked up by other workers.
 */
@Service
public class WorkerCleanupService {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkerCleanupService.class);

    private final WorkerRepository workerRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;

    @PersistenceContext
    private EntityManager entityManager;

    public WorkerCleanupService(WorkerRepository workerRepository, TaskRepository taskRepository, TaskService taskService) {
        this.workerRepository = workerRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupStaleWorkers() {

        Instant threshold = Instant.now().minusSeconds(120);

        List<Worker> staleWorkers = workerRepository.findAllByLastPingBefore(threshold);

        for (Worker worker : staleWorkers) {

            List<Task> staleTasks = entityManager.createQuery(
                            "SELECT t FROM Task t WHERE t.status = :status AND t.startedAt < :threshold",
                            Task.class)
                    .setParameter("status", EXECUTING)
                    .setParameter("threshold", threshold)
                    .getResultList();

            for (Task task : staleTasks) {
                task.setStatus(SCHEDULED);
                task.setStartedAt(null);
                task.setAssignedWorker(null);
                taskRepository.save(task);
                LOGGER.info("Freed stale task {} from worker {}", task.getId(), worker.getId());
            }
        }
    }
}