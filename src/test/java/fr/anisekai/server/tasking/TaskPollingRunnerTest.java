package fr.anisekai.server.tasking;

import fr.anisekai.server.tasking.client.ClientOrchestrator;
import fr.anisekai.server.tasking.client.TaskPollingRunner;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TaskPollingRunnerTest {

    @Test
    void startsPollingOnlyAfterApplicationIsReady() {

        ClientOrchestrator orchestrator = mock(ClientOrchestrator.class);
        TaskPollingRunner runner = new TaskPollingRunner(orchestrator);

        runner.tick();
        verifyNoInteractions(orchestrator);

        runner.enable();
        runner.tick();
        runner.tick();
        verify(orchestrator, times(2)).tick();
    }

}
