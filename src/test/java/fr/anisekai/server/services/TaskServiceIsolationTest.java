package fr.anisekai.server.services;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import fr.anisekai.scheduler.tasking.data.TaskMeta;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.repositories.TaskRepository;
import org.jetbrains.annotations.NotNull;
import fr.anisekai.server.tasking.server.ServerFactoryRegistry;
import fr.anisekai.server.tasking.server.ServerOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TaskServiceIsolationTest {

    private static final AccessScope SCOPE = new AccessScope(Library.EPISODES, "test-episode");

    @Test
    void successCommitsStagedOutputAndClearsBinding(@TempDir Path root) throws Exception {

        Fixture fixture = new Fixture(root);
        IsolationSession isolation = fixture.library.createIsolation(Set.of(SCOPE));
        Files.write(isolation.resolve(SCOPE), "converted".getBytes());
        Task task = fixture.executingTask(isolation.uuid());

        fixture.service.resolveSuccess(TaskMeta.of(task), "result");

        assertEquals(TaskStatus.SUCCEEDED, task.getStatus());
        assertNull(task.getIsolationId());
        assertTrue(Files.isRegularFile(fixture.library.resolve(SCOPE)));
    }

    @Test
    void successWithoutStagedOutputGoesThroughFailure(@TempDir Path root) {

        Fixture fixture = new Fixture(root);
        IsolationSession isolation = fixture.library.createIsolation(Set.of(SCOPE));
        Task task = fixture.executingTask(isolation.uuid());

        fixture.service.resolveSuccess(TaskMeta.of(task), "result");

        assertEquals(TaskStatus.SCHEDULED, task.getStatus());
        assertEquals(1, task.getFailureCount());
        assertNull(task.getIsolationId());
    }

    @Test
    void successWithMissingIsolationGoesThroughFailure(@TempDir Path root) {

        Fixture fixture = new Fixture(root);
        Task task = fixture.executingTask(UUID.randomUUID());

        fixture.service.resolveSuccess(TaskMeta.of(task), "result");

        assertEquals(TaskStatus.SCHEDULED, task.getStatus());
        assertEquals(1, task.getFailureCount());
        assertNull(task.getIsolationId());
    }

    @Test
    void failureDiscardsIsolationAndFreesTask(@TempDir Path root) throws Exception {

        Fixture fixture = new Fixture(root);
        IsolationSession isolation = fixture.library.createIsolation(Set.of(SCOPE));
        Files.write(isolation.resolve(SCOPE), "converted".getBytes());
        Task task = fixture.executingTask(isolation.uuid());

        fixture.service.resolveFailure(TaskMeta.of(task), new RuntimeException("boom"));

        assertEquals(TaskStatus.SCHEDULED, task.getStatus());
        assertNull(task.getIsolationId());
        assertThrows(
                RuntimeException.class,
                () -> fixture.library.getIsolatedStorage(isolation.uuid(), true)
        );
    }

    @Test
    void failureWithoutIsolationTouchesNoLibrary(@TempDir Path root) {

        Library spied = mock(Library.class, invocation -> {
            throw new UnsupportedOperationException("library must not be used");
        });

        Fixture isolated = new Fixture(root, spied);
        Task plain = isolated.executingTask(null);
        isolated.service.resolveFailure(TaskMeta.of(plain), new RuntimeException("boom"));

        assertEquals(TaskStatus.SCHEDULED, plain.getStatus());
        verifyNoInteractions(spied);
    }

    @Test
    void repeatedFailuresDiscardAndEventuallyFail(@TempDir Path root) throws Exception {

        Fixture fixture = new Fixture(root);
        IsolationSession isolation = fixture.library.createIsolation(Set.of(SCOPE));
        Files.write(isolation.resolve(SCOPE), "converted".getBytes());
        Task task = fixture.executingTask(isolation.uuid());
        task.setFailureCount((byte) 4);

        fixture.service.resolveFailure(TaskMeta.of(task), new RuntimeException("boom"));

        assertEquals(TaskStatus.FAILED, task.getStatus());
        assertNull(task.getIsolationId());
        assertThrows(
                RuntimeException.class,
                () -> fixture.library.getIsolatedStorage(isolation.uuid(), true)
        );
    }

    private static Library library(Path root) {

        ApplicationConfiguration config = new ApplicationConfiguration();
        config.getLibrary().setPath(root.toString());
        return new Library(config);
    }

    private static final class Fixture {

        private final TaskRepository repository = mock(TaskRepository.class);
        private final Library        library;
        private final TaskService    service;

        private Fixture(Path root) {

            this(root, library(root));
        }

        private Fixture(Path root, Library library) {

            TestFactory factory = new TestFactory();
            ServerFactoryRegistry registry = new ServerFactoryRegistry(List.of(factory));
            ServerOrchestrator orchestrator = new ServerOrchestrator(registry, repository);
            this.library = library;
            this.service = new TaskService(
                    repository,
                    mock(EntityEventProcessor.class),
                    orchestrator,
                    registry,
                    mock(DatabaseLockService.class),
                    library
            );
        }

        private Task executingTask(UUID isolationId) {

            Task task = new Task();
            task.setId(UUID.randomUUID());
            task.setFactoryName("iso");
            task.setName("iso-task");
            task.setStatus(TaskStatus.EXECUTING);
            task.setStartedAt(Instant.now());
            task.setArguments("input");
            task.setIsolationId(isolationId);

            when(repository.findById(task.getId())).thenReturn(Optional.of(task));
            when(repository.findAllById(any())).thenReturn(List.of(task));
            when(repository.saveAll(any())).thenReturn(List.of(task));
            return task;
        }
    }

    private static final class TestFactory implements ServerFactory<Task, String, String>, fr.anisekai.server.tasking.IsolatedServerFactory<String> {

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
        public @NotNull java.util.Set<fr.anisekai.sanctum.AccessScope> getIsolationScopes(
                @NotNull Task task, @NotNull String input) {

            return Set.of(SCOPE);
        }
    }
}
