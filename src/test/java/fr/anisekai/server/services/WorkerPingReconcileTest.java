package fr.anisekai.server.services;

import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.library.Library;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.data.TaskFailedPacket;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.tasking.server.ServerFactoryRegistry;
import fr.anisekai.server.tasking.server.ServerOrchestrator;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.web.dto.WorkerDirective;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerPingReconcileTest {

    @Test
    void assignsFreshTaskWhenBothIdle() {

        Fixture fixture = new Fixture();
        Task fresh = fixture.scheduledTask();
        when(fixture.repository.findAllByAssignedWorkerAndStatus(eq(fixture.worker), eq(TaskStatus.EXECUTING)))
                .thenReturn(List.of());

        WorkerPollResult result = fixture.service.reconcileAndPoll(fixture.worker, List.of("test"), null);

        assertTrue(result.assigned().isPresent());
        assertSame(fresh, result.assigned().get());
        assertSame(fixture.worker, fresh.getAssignedWorker());
        assertEquals(WorkerDirective.NONE, result.directive());
    }

    @Test
    void acksInSyncStateWithoutPolling() {

        Fixture fixture = new Fixture();
        Task task = fixture.executingTask();
        when(fixture.repository.findAllByAssignedWorkerAndStatus(eq(fixture.worker), eq(TaskStatus.EXECUTING)))
                .thenReturn(List.of(task));

        WorkerPollResult result = fixture.service.reconcileAndPoll(fixture.worker, List.of("test"), task.getId());

        assertTrue(result.assigned().isEmpty());
        assertEquals(WorkerDirective.NONE, result.directive());
        assertEquals(0, fixture.factory.assignments.get());
        verify(fixture.repository, never()).findById(any());
        verify(fixture.repository, never()).save(any());
    }

    @Test
    void tellsBusyWorkerToGiveUpWithoutTouchingDatabase() {

        Fixture fixture = new Fixture();
        when(fixture.repository.findAllByAssignedWorkerAndStatus(eq(fixture.worker), eq(TaskStatus.EXECUTING)))
                .thenReturn(List.of());

        WorkerPollResult result = fixture.service.reconcileAndPoll(fixture.worker, List.of("test"), UUID.randomUUID());

        assertTrue(result.assigned().isEmpty());
        assertEquals(WorkerDirective.GIVE_UP_TASK, result.directive());
        assertEquals(0, fixture.factory.assignments.get());
        verify(fixture.repository, never()).findById(any());
        verify(fixture.repository, never()).save(any());
    }

    @Test
    void failsOrphanAndAssignsFreshWhenIdle() {

        Fixture fixture = new Fixture();
        Task orphan = fixture.executingTask();
        Task fresh = fixture.scheduledTask();
        when(fixture.repository.findAllByAssignedWorkerAndStatus(eq(fixture.worker), eq(TaskStatus.EXECUTING)))
                .thenReturn(List.of(orphan));

        WorkerPollResult result = fixture.service.reconcileAndPoll(fixture.worker, List.of("test"), null);

        assertEquals(TaskStatus.SCHEDULED, orphan.getStatus());
        assertEquals(1, orphan.getFailureCount());
        assertTrue(result.assigned().isPresent());
        assertSame(fresh, result.assigned().get());
        assertEquals(WorkerDirective.NONE, result.directive());
    }

    @Test
    void failsMismatchAndTellsWorkerToGiveUpWithoutAssigning() {

        Fixture fixture = new Fixture();
        Task claimed = fixture.executingTask();
        when(fixture.repository.findAllByAssignedWorkerAndStatus(eq(fixture.worker), eq(TaskStatus.EXECUTING)))
                .thenReturn(List.of(claimed));

        WorkerPollResult result = fixture.service.reconcileAndPoll(fixture.worker, List.of("test"), UUID.randomUUID());

        assertEquals(TaskStatus.SCHEDULED, claimed.getStatus());
        assertEquals(1, claimed.getFailureCount());
        assertTrue(result.assigned().isEmpty());
        assertEquals(WorkerDirective.GIVE_UP_TASK, result.directive());
        assertEquals(0, fixture.factory.assignments.get());
    }

    @Test
    void keepsReportedTaskAndFailsExtras() {

        Fixture fixture = new Fixture();
        Task mine = fixture.executingTask();
        Task extra = fixture.executingTask();
        when(fixture.repository.findAllByAssignedWorkerAndStatus(eq(fixture.worker), eq(TaskStatus.EXECUTING)))
                .thenReturn(List.of(mine, extra));

        WorkerPollResult result = fixture.service.reconcileAndPoll(fixture.worker, List.of("test"), mine.getId());

        assertEquals(TaskStatus.EXECUTING, mine.getStatus());
        assertSame(fixture.worker, mine.getAssignedWorker());
        assertEquals(TaskStatus.SCHEDULED, extra.getStatus());
        assertEquals(1, extra.getFailureCount());
        assertTrue(result.assigned().isEmpty());
        assertEquals(WorkerDirective.NONE, result.directive());
        assertEquals(0, fixture.factory.assignments.get());
    }

    private static final class Fixture {

        private final TaskRepository repository = mock(TaskRepository.class);
        private final TestFactory    factory    = new TestFactory();
        private final TaskService    service;
        private final Worker         worker = worker();

        private Fixture() {

            ServerFactoryRegistry registry = new ServerFactoryRegistry(List.of(factory));
            ServerOrchestrator orchestrator = new ServerOrchestrator(registry, repository);
            this.service = new TaskService(
                    repository,
                    mock(EntityEventProcessor.class),
                    orchestrator,
                    registry,
                    mock(DatabaseLockService.class),
                    mock(Library.class)
            );
        }

        private Task scheduledTask() {

            Task task = new Task();
            task.setId(UUID.randomUUID());
            task.setFactoryName("test");
            task.setName("test-task-" + task.getId());
            task.setStatus(TaskStatus.SCHEDULED);
            ReflectionTestUtils.setField(task, "createdAt", Instant.now());

            when(repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                    .thenReturn(List.of(task));
            when(repository.claim(eq(task.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                    .thenReturn(1);
            return task;
        }

        private Task executingTask() {

            Task task = new Task();
            task.setId(UUID.randomUUID());
            task.setFactoryName("test");
            task.setName("test-task-" + task.getId());
            task.setStatus(TaskStatus.EXECUTING);
            task.setStartedAt(Instant.now());
            task.setAssignedWorker(worker);
            ReflectionTestUtils.setField(task, "createdAt", Instant.now());

            when(repository.findById(task.getId())).thenReturn(Optional.of(task));
            when(repository.findAllById(any())).thenReturn(List.of(task));
            when(repository.saveAll(any())).thenReturn(List.of(task));
            return task;
        }

        private static Worker worker() {

            Worker worker = new Worker();
            worker.setId(UUID.randomUUID());
            worker.setLastPing(Instant.now());
            return worker;
        }
    }

    private static final class TestFactory implements ServerFactory<Task, String, String> {

        private static final ObjectSerializer<String> SERIALIZER = new ObjectSerializer<>() {
            @Override
            public String serialize(String object) {

                return object;
            }

            @Override
            public String deserialize(String data) {

                return data;
            }
        };

        private final AtomicInteger assignments = new AtomicInteger();

        @Override
        public String getName() {

            return "test";
        }

        @Override
        public ObjectSerializer<String> getArgumentsSerializer() {

            return SERIALIZER;
        }

        @Override
        public ObjectSerializer<String> getResultSerializer() {

            return SERIALIZER;
        }

        @Override
        public void onAssigningTask(fr.anisekai.scheduler.tasking.data.TaskExecutionPacket<Task> packet) {

            this.assignments.incrementAndGet();
        }

        @Override
        public void onSuccess(TaskExecutedPacket<Task, String> packet) {
        }

        @Override
        public void onFailure(TaskFailedPacket<Task> packet) {
        }
    }
}
