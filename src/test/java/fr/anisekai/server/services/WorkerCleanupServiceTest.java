package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.WorkerRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerCleanupServiceTest {

    @Test
    void releasesAndClearsStaleLeases() {

        WorkerRepository workers = mock(WorkerRepository.class);
        WorkerService workerService = mock(WorkerService.class);
        WorkerCleanupService cleanupService = new WorkerCleanupService(workers, workerService);
        Worker worker = new Worker();
        worker.setId(UUID.randomUUID());
        worker.setLastPing(Instant.now().minusSeconds(3600));
        worker.setInstanceId(UUID.randomUUID());
        Task task = new Task();
        task.setId(UUID.randomUUID());
        when(workers.findAllStaleLeasedForUpdate(any())).thenReturn(List.of(worker));
        when(workerService.release(worker)).thenReturn(List.of(task));

        cleanupService.cleanupStaleWorkers();

        assertNull(worker.getInstanceId());
        verify(workerService).release(worker);
        verify(workers).save(worker);
    }

    @Test
    void leavesWorkersAloneWhenNoStaleLeaseIsLocked() {

        WorkerRepository workers = mock(WorkerRepository.class);
        WorkerService workerService = mock(WorkerService.class);
        WorkerCleanupService cleanupService = new WorkerCleanupService(workers, workerService);
        when(workers.findAllStaleLeasedForUpdate(any())).thenReturn(List.of());

        cleanupService.cleanupStaleWorkers();

        verify(workerService, never()).release(any());
        verify(workers, never()).save(any());
    }
}
