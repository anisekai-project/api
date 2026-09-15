package fr.anisekai.server.tasking;

import fr.anisekai.scheduler.tasking.data.TaskMeta;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.FactoryRegistry;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskClient;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.tasking.client.ClientOrchestrator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientOrchestratorTest {

    @Test
    void resolvesSuccessfulExecution() throws Exception {

        Fixture fixture = new Fixture();
        when(fixture.server.poll(any(TaskClient.class))).thenReturn(Optional.of(fixture.task));
        when(fixture.factory.execute(any(TaskMeta.class))).thenReturn("result");

        fixture.orchestrator.tick();

        verify(fixture.taskService).resolveSuccess(new TaskMeta(fixture.task.getId(), "test", "argument"), "result");
    }

    @Test
    void resolvesFailedExecution() throws Exception {

        Fixture fixture = new Fixture();
        IllegalStateException failure = new IllegalStateException("failure");
        when(fixture.server.poll(any(TaskClient.class))).thenReturn(Optional.of(fixture.task));
        when(fixture.factory.execute(any(TaskMeta.class))).thenThrow(failure);

        fixture.orchestrator.tick();

        verify(fixture.taskService).resolveFailure(new TaskMeta(fixture.task.getId(), "test", "argument"), failure);
    }

    @Test
    void advertisesRegisteredLocalFactories() {

        Fixture fixture = new Fixture();

        fixture.orchestrator.poll();

        verify(fixture.server).poll(fixture.clientCaptor.capture());
        assertEquals(List.of(fixture.factory), fixture.clientCaptor.getValue().getSupportedFactories());
    }

    @SuppressWarnings("unchecked")
    private static final class Fixture {

        private final FactoryRegistry<ClientFactory<?, ?>> registry = mock(FactoryRegistry.class);
        private final fr.anisekai.scheduler.tasking.interfaces.orchestrator.ServerOrchestrator<Task> server = mock(fr.anisekai.scheduler.tasking.interfaces.orchestrator.ServerOrchestrator.class);
        private final TaskService taskService = mock(TaskService.class);
        private final ClientFactory<String, String> factory = mock(ClientFactory.class);
        private final org.mockito.ArgumentCaptor<TaskClient> clientCaptor = org.mockito.ArgumentCaptor.forClass(TaskClient.class);
        private final Task task = task();
        private final ClientOrchestrator orchestrator;

        private Fixture() {

            when(this.registry.getFactories()).thenReturn(List.of(this.factory));
            doReturn(this.factory).when(this.registry).query("test");
            this.orchestrator = new ClientOrchestrator(this.registry, this.server, this.taskService);
        }

        private static Task task() {

            Task task = new Task();
            task.setId(UUID.randomUUID());
            task.setFactoryName("test");
            task.setName("test");
            task.setArguments("argument");
            task.setStatus(TaskStatus.EXECUTING);
            return task;
        }
    }

}
