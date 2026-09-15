package fr.anisekai.server.services;

import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.data.TaskFailedPacket;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.tasking.IsolatedServerFactory;
import fr.anisekai.server.tasking.server.ServerFactoryRegistry;
import fr.anisekai.server.tasking.server.ServerOrchestrator;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.web.exceptions.WebException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceWorkerPollTest {

    @Test
    void claimsThroughOrchestratorAndRecordsWorker() {

        Fixture fixture = new Fixture();
        when(fixture.repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                .thenReturn(List.of(fixture.task));
        when(fixture.repository.claim(eq(fixture.task.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                .thenReturn(1);

        Optional<Task> claimed = fixture.service.pollForWorker(fixture.worker, List.of("test"));

        assertTrue(claimed.isPresent());
        assertSame(fixture.task, claimed.get());
        assertEquals(TaskStatus.EXECUTING, fixture.task.getStatus());
        assertSame(fixture.worker, fixture.task.getAssignedWorker());
        assertEquals(1, fixture.factory.assignments.get());
        verify(fixture.repository).save(fixture.task);
    }

    @Test
    void rejectsEmptyFactoryDeclarations() {

        Fixture fixture = new Fixture();

        WebException ex = assertThrows(WebException.class,
                () -> fixture.service.pollForWorker(fixture.worker, List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
    }

    @Test
    void rejectsUnknownFactories() {

        Fixture fixture = new Fixture();

        WebException ex = assertThrows(WebException.class,
                () -> fixture.service.pollForWorker(fixture.worker, List.of("unknown")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
    }

    @Test
    void returnsEmptyWhenNothingSchedulable() {

        Fixture fixture = new Fixture();
        when(fixture.repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                .thenReturn(List.of());

        assertTrue(fixture.service.pollForWorker(fixture.worker, List.of("test")).isEmpty());
        assertEquals(0, fixture.factory.assignments.get());
    }

    @Test
    void createsIsolationForIsolatedFactoryAndRecordsIt() {

        TaskRepository repository = mock(TaskRepository.class);
        IsolatedTestFactory factory = new IsolatedTestFactory();
        ServerFactoryRegistry registry = new ServerFactoryRegistry(List.of(factory));
        ServerOrchestrator realOrchestrator = new ServerOrchestrator(registry, repository);
        Library library = mock(Library.class);
        TaskService service = new TaskService(
                repository,
                mock(EntityEventProcessor.class),
                realOrchestrator,
                registry,
                mock(DatabaseLockService.class),
                library
        );

        UUID isolationId = UUID.randomUUID();
        IsolationSession isolation = mock(IsolationSession.class);
        when(isolation.uuid()).thenReturn(isolationId);
        when(library.createIsolation(any(SessionToken.class), any(AccessScope[].class))).thenReturn(isolation);

        SessionToken token = new SessionToken();
        token.setId(UUID.randomUUID());
        Worker worker = new Worker();
        worker.setId(UUID.randomUUID());
        worker.setSessionToken(token);
        worker.setLastPing(Instant.now());

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setFactoryName("iso");
        task.setName("iso-task");
        task.setStatus(TaskStatus.SCHEDULED);
        task.setArguments("input");
        ReflectionTestUtils.setField(task, "createdAt", Instant.now());

        when(repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED))
                .thenReturn(List.of(task));
        when(repository.claim(eq(task.getId()), eq(TaskStatus.SCHEDULED), eq(TaskStatus.EXECUTING), any()))
                .thenReturn(1);

        Optional<Task> claimed = service.pollForWorker(worker, List.of("iso"));

        assertTrue(claimed.isPresent());
        assertSame(worker, task.getAssignedWorker());
        assertEquals(isolationId, task.getIsolationId());
        verify(library).createIsolation(eq(token), any(AccessScope[].class));
        verify(repository).save(task);
    }

    private static final class Fixture {

        private final TaskRepository repository = mock(TaskRepository.class);
        private final TestFactory factory = new TestFactory();
        private final TaskService service;
        private final Task task = task();
        private final Worker worker = worker();

        private Fixture() {

            // Real registry so factory resolution behaves like production.
            ServerFactoryRegistry registry = new ServerFactoryRegistry(List.of(factory));
            // Rewire the orchestrator mock registry to the real one via a fresh orchestrator.
            ServerOrchestrator realOrchestrator = new ServerOrchestrator(registry, repository);
            this.service = new TaskService(
                    repository,
                    mock(EntityEventProcessor.class),
                    realOrchestrator,
                    registry,
                    mock(DatabaseLockService.class),
                    mock(Library.class)
            );
        }

        private static Task task() {

            Task task = new Task();
            task.setId(UUID.randomUUID());
            task.setFactoryName("test");
            task.setName("test-task");
            task.setStatus(TaskStatus.SCHEDULED);
            ReflectionTestUtils.setField(task, "createdAt", Instant.now());
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

    private static final class IsolatedTestFactory implements ServerFactory<Task, String, String>, IsolatedServerFactory<String> {

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

        @Override
        public String getName() {

            return "iso";
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
        public @NotNull Set<AccessScope> getIsolationScopes(@NotNull Task task, @NotNull String input) {

            return Set.of(new AccessScope(Library.EPISODES, "scope"));
        }
    }
}
