package fr.anisekai.server.tasking;

import fr.anisekai.scheduler.commons.ActionPlan;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.ReservedTaskMeta;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.data.TaskFailedPacket;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.scheduler.tasking.interfaces.factories.Factory;
import fr.anisekai.scheduler.tasking.interfaces.factories.FactoryRegistry;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskClient;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.tasking.server.ServerOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerOrchestratorTest {

    @Test
    void skipsTaskLostToAnotherClientAndClaimsNextTask() {

        TaskRepository repository = mock(TaskRepository.class);
        TestFactory factory = new TestFactory();
        ServerOrchestrator orchestrator = new ServerOrchestrator(registry(factory), repository);
        Task first = task("first", TaskStatus.SCHEDULED);
        Task second = task("second", TaskStatus.SCHEDULED);
        TaskClient client = client(factory);

        when(repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                .thenReturn(List.of(first, second));
        when(repository.claim(eq(first.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                .thenReturn(0);
        when(repository.claim(eq(second.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                .thenReturn(1);

        assertSame(second, orchestrator.poll(client).orElseThrow());
        assertEquals(TaskStatus.SCHEDULED, first.getStatus());
        assertNull(first.getStartedAt());
        assertEquals(TaskStatus.EXECUTING, second.getStatus());
        assertNotNull(second.getStartedAt());
        assertEquals(1, factory.assignments.get());
    }

    @Test
    void failedClaimDoesNotMutateTask() {

        TaskRepository repository = mock(TaskRepository.class);
        TestFactory factory = new TestFactory();
        ServerOrchestrator orchestrator = new ServerOrchestrator(registry(factory), repository);
        Task task = task("test", TaskStatus.SCHEDULED);

        when(repository.claim(eq(task.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                .thenReturn(0);

        assertFalse(orchestrator.claim(task, client(factory)));
        assertEquals(TaskStatus.SCHEDULED, task.getStatus());
        assertNull(task.getStartedAt());
        assertEquals(0, factory.assignments.get());
    }

    @Test
    void appliesSuccessLifecycleAndFactoryCallback() {

        TaskRepository repository = mock(TaskRepository.class);
        TestFactory factory = new TestFactory();
        ServerOrchestrator orchestrator = new ServerOrchestrator(registry(factory), repository);
        Task task = task("test", TaskStatus.EXECUTING);

        ActionPlan<UUID, ReservedTaskMeta, Task> plan = orchestrator.resolve(new TaskExecutedPacket<>(task, "result"));
        plan.updates().getFirst().hook().accept(task);

        assertEquals(TaskStatus.SUCCEEDED, task.getStatus());
        assertNotNull(task.getCompletedAt());
        assertNull(ReflectionTestUtils.getField(task, "activeKey"));
        assertEquals(1, factory.successes.get());
    }

    @Test
    void retriesFailureAndEventuallyMarksTaskFailed() {

        TaskRepository repository = mock(TaskRepository.class);
        TestFactory factory = new TestFactory();
        ServerOrchestrator orchestrator = new ServerOrchestrator(registry(factory), repository);
        Task task = task("test", TaskStatus.EXECUTING);

        for (int failure = 1; failure <= 5; failure++) {
            ActionPlan<UUID, ReservedTaskMeta, Task> plan = orchestrator.resolve(
                    new TaskFailedPacket<>(task, new IllegalStateException("failure " + failure))
            );
            plan.updates().getFirst().hook().accept(task);

            assertEquals((byte) failure, task.getFailureCount());
            assertEquals(failure == 5 ? TaskStatus.FAILED : TaskStatus.SCHEDULED, task.getStatus());
            if (failure < 5) task.setStatus(TaskStatus.EXECUTING);
        }

        assertNotNull(task.getCompletedAt());
        assertNull(ReflectionTestUtils.getField(task, "activeKey"));
        assertEquals(5, factory.failures.get());
    }

    @Test
    void readsOnlyScheduledTasksInDispatchOrder() {

        TaskRepository repository = mock(TaskRepository.class);
        ServerOrchestrator orchestrator = new ServerOrchestrator(registry(new TestFactory()), repository);
        List<Task> tasks = List.of(task("test", TaskStatus.SCHEDULED));
        when(repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                .thenReturn(tasks);

        assertSame(tasks, orchestrator.getTasks());
        verify(repository).findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED);
        verify(repository, never()).findAll();
    }

    private static Task task(String name, TaskStatus status) {

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setFactoryName("test");
        task.setName(name);
        task.setStatus(status);
        ReflectionTestUtils.setField(task, "createdAt", Instant.now());
        return task;
    }

    private static TaskClient client(TestFactory factory) {

        return new TaskClient() {
            @Override
            public UUID getId() {

                return UUID.randomUUID();
            }

            @Override
            public Collection<Factory<?, ?>> getSupportedFactories() {

                return List.of(factory);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static FactoryRegistry<ServerFactory<Task, ?, ?>> registry(TestFactory factory) {

        FactoryRegistry<ServerFactory<Task, ?, ?>> registry = mock(FactoryRegistry.class);
        doReturn(factory).when(registry).query("test");
        return registry;
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
        private final AtomicInteger successes = new AtomicInteger();
        private final AtomicInteger failures = new AtomicInteger();

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

            this.successes.incrementAndGet();
        }

        @Override
        public void onFailure(TaskFailedPacket<Task> packet) {

            this.failures.incrementAndGet();
        }
    }

}
