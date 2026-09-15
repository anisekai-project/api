package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service that periodically releases workers that have not pinged the API within the threshold.
 * Freed tasks return to the schedule so they can be picked up on the next heartbeat.
 */
@Service
public class WorkerCleanupService {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkerCleanupService.class);

    private final WorkerRepository workerRepository;
    private final WorkerService    workerService;

    public WorkerCleanupService(WorkerRepository workerRepository, WorkerService workerService) {
        this.workerRepository = workerRepository;
        this.workerService    = workerService;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupStaleWorkers() {

        Instant threshold = Instant.now().minus(WorkerService.STALE_AFTER);

        List<Worker> staleWorkers = workerRepository.findAllByLastPingBefore(threshold);

        for (Worker worker : staleWorkers) {
            for (Task task : this.workerService.release(worker)) {
                LOGGER.info("Freed stale task {} from worker {}", task.getId(), worker.getId());
            }
        }
    }
}
