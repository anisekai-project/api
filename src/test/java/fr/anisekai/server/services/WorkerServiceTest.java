package fr.anisekai.server.services;

import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.library.Library;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.repositories.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerServiceTest {

    @Test
    void provisionBindsWorkerIdToTokenId() {

        Fixture fixture = new Fixture();
        SessionToken token = token(UUID.randomUUID());
        when(fixture.workers.save(any(Worker.class))).thenAnswer(call -> call.getArgument(0));

        Worker worker = fixture.service.provision(token);

        assertEquals(token.getId(), worker.getId());
        assertSame(token, worker.getSessionToken());
        assertNull(worker.getInstanceId());
        verify(fixture.workers).save(worker);
    }

    @Test
    void freshnessFollowsStaleThreshold() {

        Fixture fixture = new Fixture();
        Instant now = Instant.now();

        assertFalse(fixture.service.isStale(workerPingedAt(now.minusSeconds(119)), now));
        assertTrue(fixture.service.isStale(workerPingedAt(now.minusSeconds(121)), now));
    }

    @Test
    void heartbeatRefreshesPingAndName() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now().minusSeconds(3600));
        worker.setName("old");
        when(fixture.workers.save(any(Worker.class))).thenAnswer(call -> call.getArgument(0));
        Instant before = Instant.now();

        Worker updated = fixture.service.heartbeat(worker, "new");

        assertFalse(updated.getLastPing().isBefore(before));
        assertEquals("new", updated.getName());
    }

    @Test
    void heartbeatBlankNameKeepsCurrentName() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now().minusSeconds(3600));
        worker.setName("old");
        when(fixture.workers.save(any(Worker.class))).thenAnswer(call -> call.getArgument(0));

        Worker updated = fixture.service.heartbeat(worker, "  ");

        assertEquals("old", updated.getName());
    }

    @Test
    void firstClaimSucceedsDespiteFreshProvisionPing() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now());
        UUID instanceId = UUID.randomUUID();
        when(fixture.workers.save(any(Worker.class))).thenAnswer(call -> call.getArgument(0));

        boolean claimed = fixture.service.claim(worker, instanceId, "worker");

        assertTrue(claimed);
        assertEquals(instanceId, worker.getInstanceId());
        assertEquals("worker", worker.getName());
    }

    @Test
    void sameInstanceRenewsActiveLease() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now());
        UUID instanceId = UUID.randomUUID();
        worker.setInstanceId(instanceId);
        when(fixture.workers.save(any(Worker.class))).thenAnswer(call -> call.getArgument(0));

        boolean claimed = fixture.service.claim(worker, instanceId, "worker");

        assertTrue(claimed);
        assertEquals(instanceId, worker.getInstanceId());
        verify(fixture.workers).save(worker);
    }

    @Test
    void differentInstanceCannotClaimActiveLease() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now());
        UUID currentInstanceId = UUID.randomUUID();
        Instant lastPing = worker.getLastPing();
        worker.setInstanceId(currentInstanceId);
        worker.setName("current");

        boolean claimed = fixture.service.claim(worker, UUID.randomUUID(), "replacement");

        assertFalse(claimed);
        assertEquals(currentInstanceId, worker.getInstanceId());
        assertEquals(lastPing, worker.getLastPing());
        assertEquals("current", worker.getName());
        verify(fixture.workers, never()).save(any());
        verify(fixture.tasks, never()).findAllByAssignedWorkerAndStatus(any(), any());
    }

    @Test
    void differentInstanceReleasesStaleLeaseBeforeClaiming() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now().minusSeconds(3600));
        worker.setInstanceId(UUID.randomUUID());
        Task task = executingTask(worker);
        UUID isolationId = UUID.randomUUID();
        UUID replacementInstanceId = UUID.randomUUID();
        task.setIsolationId(isolationId);
        when(fixture.tasks.findAllByAssignedWorkerAndStatus(worker, TaskStatus.EXECUTING))
                .thenReturn(List.of(task));
        when(fixture.tasks.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        when(fixture.workers.save(any(Worker.class))).thenAnswer(call -> call.getArgument(0));

        boolean claimed = fixture.service.claim(worker, replacementInstanceId, "replacement");

        assertTrue(claimed);
        assertEquals(replacementInstanceId, worker.getInstanceId());
        assertEquals(TaskStatus.SCHEDULED, task.getStatus());
        assertNull(task.getAssignedWorker());
        assertNull(task.getIsolationId());
        verify(fixture.library).discardIsolation(isolationId);
    }

    @Test
    void releaseFreesOnlyWorkerExecutingTasks() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now());
        Task first = executingTask(worker);
        Task second = executingTask(worker);
        when(fixture.tasks.findAllByAssignedWorkerAndStatus(worker, TaskStatus.EXECUTING))
                .thenReturn(List.of(first, second));
        when(fixture.tasks.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        List<Task> freed = fixture.service.release(worker);

        assertEquals(2, freed.size());
        for (Task task : freed) {
            assertEquals(TaskStatus.SCHEDULED, task.getStatus());
            assertNull(task.getStartedAt());
            assertNull(task.getAssignedWorker());
        }
        verify(fixture.tasks).findAllByAssignedWorkerAndStatus(worker, TaskStatus.EXECUTING);
    }

    @Test
    void releaseDiscardsBoundIsolation() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now());
        Task task = executingTask(worker);
        UUID isolationId = UUID.randomUUID();
        task.setIsolationId(isolationId);
        when(fixture.tasks.findAllByAssignedWorkerAndStatus(worker, TaskStatus.EXECUTING))
                .thenReturn(List.of(task));
        when(fixture.tasks.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        fixture.service.release(worker);

        verify(fixture.library).discardIsolation(isolationId);
        assertNull(task.getIsolationId());
        assertEquals(TaskStatus.SCHEDULED, task.getStatus());
    }

    @Test
    void releaseDefersIsolationDiscardUntilCommit() {

        Fixture fixture = new Fixture();
        Worker worker = workerPingedAt(Instant.now());
        Task task = executingTask(worker);
        UUID isolationId = UUID.randomUUID();
        task.setIsolationId(isolationId);
        when(fixture.tasks.findAllByAssignedWorkerAndStatus(worker, TaskStatus.EXECUTING))
                .thenReturn(List.of(task));
        when(fixture.tasks.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            fixture.service.release(worker);

            verify(fixture.library, never()).discardIsolation(isolationId);
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            verify(fixture.library).discardIsolation(isolationId);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static SessionToken token(UUID id) {

        SessionToken token = new SessionToken();
        token.setId(id);
        return token;
    }

    private static Worker workerPingedAt(Instant lastPing) {

        UUID id = UUID.randomUUID();
        Worker worker = new Worker();
        worker.setId(id);
        worker.setSessionToken(token(id));
        worker.setLastPing(lastPing);
        return worker;
    }

    private static Task executingTask(Worker worker) {

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setFactoryName("test");
        task.setName("test-task-" + task.getId());
        task.setStatus(TaskStatus.EXECUTING);
        task.setStartedAt(Instant.now().minusSeconds(3600));
        task.setAssignedWorker(worker);
        return task;
    }

    private static final class Fixture {

        private final WorkerRepository workers = mock(WorkerRepository.class);
        private final TaskRepository   tasks   = mock(TaskRepository.class);
        private final Library          library = mock(Library.class);
        private final WorkerService    service = new WorkerService(
                workers,
                mock(EntityEventProcessor.class),
                tasks,
                library
        );
    }
}
