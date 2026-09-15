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
import java.util.Optional;
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
    void activeKeyHoldsTaskNameWithoutFactoryPrefixDuplication() {

        Task task = task("media:convert:0000", TaskStatus.SCHEDULED);

        assertEquals("media:convert:0000", task.getActiveKey());
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

    @Test
    void skipsConvertWhileStoreCounterpartExecutes() {

        TaskRepository repository = mock(TaskRepository.class);
        ServerFactory<Task, ?, ?> convertFactory = factory("media:convert");
        ServerFactory<Task, ?, ?> otherFactory = factory("test");
        ServerOrchestrator orchestrator = new ServerOrchestrator(registry(convertFactory, otherFactory), repository);
        UUID episodeId = UUID.randomUUID();
        Task convert = task("media:convert", "media:convert:" + episodeId, TaskStatus.SCHEDULED);
        Task other = task("test", "other", TaskStatus.SCHEDULED);

        when(repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                .thenReturn(List.of(convert, other));
        when(repository.findFirstByFactoryNameAndNameAndStatusIn(
                "media:store", "media:store:" + episodeId, List.of(TaskStatus.EXECUTING)))
                .thenReturn(Optional.of(task("media:store", "media:store:" + episodeId, TaskStatus.EXECUTING)));
        when(repository.claim(eq(other.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                .thenReturn(1);

        assertSame(other, orchestrator.poll(client(convertFactory, otherFactory)).orElseThrow());
        verify(repository, never()).claim(
                eq(convert.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any());
        assertEquals(TaskStatus.SCHEDULED, convert.getStatus());
    }

    @Test
    void skipsStoreWhileConvertCounterpartExecutes() {

        TaskRepository repository = mock(TaskRepository.class);
        ServerFactory<Task, ?, ?> storeFactory = factory("media:store");
        ServerFactory<Task, ?, ?> otherFactory = factory("test");
        ServerOrchestrator orchestrator = new ServerOrchestrator(registry(storeFactory, otherFactory), repository);
        UUID episodeId = UUID.randomUUID();
        Task store = task("media:store", "media:store:" + episodeId, TaskStatus.SCHEDULED);
        Task other = task("test", "other", TaskStatus.SCHEDULED);

        when(repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                .thenReturn(List.of(store, other));
        when(repository.findFirstByFactoryNameAndNameAndStatusIn(
                "media:convert", "media:convert:" + episodeId, List.of(TaskStatus.EXECUTING)))
                .thenReturn(Optional.of(task("media:convert", "media:convert:" + episodeId, TaskStatus.EXECUTING)));
        when(repository.claim(eq(other.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                .thenReturn(1);

        assertSame(other, orchestrator.poll(client(storeFactory, otherFactory)).orElseThrow());
        verify(repository, never()).claim(
                eq(store.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any());
        assertEquals(TaskStatus.SCHEDULED, store.getStatus());
    }

    @Test
    void proceedsWhenNoCounterpartExecutes() {

        TaskRepository repository = mock(TaskRepository.class);
        ServerFactory<Task, ?, ?> convertFactory = factory("media:convert");
        ServerOrchestrator orchestrator = new ServerOrchestrator(registry(convertFactory), repository);
        UUID episodeId = UUID.randomUUID();
        Task convert = task("media:convert", "media:convert:" + episodeId, TaskStatus.SCHEDULED);

        when(repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                .thenReturn(List.of(convert));
        when(repository.findFirstByFactoryNameAndNameAndStatusIn(
                "media:store", "media:store:" + episodeId, List.of(TaskStatus.EXECUTING)))
                .thenReturn(Optional.empty());
        when(repository.claim(eq(convert.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                .thenReturn(1);

        assertSame(convert, orchestrator.poll(client(convertFactory)).orElseThrow());
        assertEquals(TaskStatus.EXECUTING, convert.getStatus());
    }

    private static Task task(String name, TaskStatus status) {

        return task("test", name, status);
    }

    private static Task task(String factoryName, String name, TaskStatus status) {

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setFactoryName(factoryName);
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

    @SuppressWarnings("unchecked")
    private static ServerFactory<Task, ?, ?> factory(String name) {

        ServerFactory<Task, ?, ?> factory = mock(ServerFactory.class);
        doReturn(name).when(factory).getName();
        return factory;
    }

    @SuppressWarnings("unchecked")
    private static FactoryRegistry<ServerFactory<Task, ?, ?>> registry(ServerFactory<Task, ?, ?>... factories) {

        FactoryRegistry<ServerFactory<Task, ?, ?>> registry = mock(FactoryRegistry.class);
        for (ServerFactory<Task, ?, ?> factory : factories) {
            String name = factory.getName();
            doReturn(factory).when(registry).query(name);
        }
        return registry;
    }

    private static TaskClient client(ServerFactory<Task, ?, ?>... factories) {

        return new TaskClient() {
            @Override
            public UUID getId() {

                return UUID.randomUUID();
            }

            @Override
            public Collection<Factory<?, ?>> getSupportedFactories() {

                return List.of(factories);
            }
        };
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
