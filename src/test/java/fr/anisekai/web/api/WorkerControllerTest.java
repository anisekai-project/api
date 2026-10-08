package fr.anisekai.web.api;

import fr.anisekai.scheduler.tasking.data.TaskMeta;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.repositories.WorkerRepository;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.services.WorkerService;
import fr.anisekai.web.dto.TaskCompletionRequest;
import fr.anisekai.web.dto.WorkerPingRequest;
import fr.anisekai.web.exceptions.WebException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerControllerTest {

    @Test
    void rejectsActiveDifferentInstanceBeforeReconciliation() {

        Fixture fixture = new Fixture();
        UUID instanceId = UUID.randomUUID();
        when(fixture.workers.findForUpdateById(fixture.session.getId())).thenReturn(Optional.of(fixture.worker));
        when(fixture.workerService.claim(fixture.worker, instanceId, "replacement")).thenReturn(false);

        WebException exception = assertThrows(WebException.class, () -> fixture.controller.ping(
                new WorkerPingRequest(instanceId, List.of("test"), "replacement", null),
                fixture.session
        ));

        assertEquals(HttpStatus.CONFLICT, exception.status);
        verify(fixture.workers).findForUpdateById(fixture.session.getId());
        verify(fixture.taskService, never()).reconcileAndPoll(any(), any(), any());
    }

    @Test
    void rejectsCompletionFromObsoleteInstanceBeforeTaskLookup() {

        Fixture fixture = new Fixture();
        UUID taskId = UUID.randomUUID();
        fixture.worker.setInstanceId(UUID.randomUUID());
        when(fixture.workers.findForUpdateById(fixture.session.getId())).thenReturn(Optional.of(fixture.worker));

        WebException exception = assertThrows(WebException.class, () -> fixture.controller.reportSuccess(
                taskId,
                new TaskCompletionRequest(UUID.randomUUID(), "result", null),
                fixture.session
        ));

        assertEquals(HttpStatus.CONFLICT, exception.status);
        verify(fixture.workers).findForUpdateById(fixture.session.getId());
        verify(fixture.tasks, never()).findById(taskId);
        verify(fixture.taskService, never()).resolveSuccess(any(), any());
    }

    @Test
    void acceptsCompletionFromCurrentInstance() {

        Fixture fixture = new Fixture();
        UUID instanceId = UUID.randomUUID();
        fixture.worker.setInstanceId(instanceId);
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setFactoryName("test");
        task.setName("test-task");
        task.setStatus(TaskStatus.EXECUTING);
        task.setAssignedWorker(fixture.worker);
        when(fixture.workers.findForUpdateById(fixture.session.getId())).thenReturn(Optional.of(fixture.worker));
        when(fixture.tasks.findById(task.getId())).thenReturn(Optional.of(task));

        ResponseEntity<?> response = fixture.controller.reportSuccess(
                task.getId(),
                new TaskCompletionRequest(instanceId, "result", null),
                fixture.session
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fixture.taskService).resolveSuccess(eq(TaskMeta.of(task)), eq("result"));
    }

    @Test
    void completedTaskIsNotResolvedAgain() {

        Fixture fixture = new Fixture();
        UUID instanceId = UUID.randomUUID();
        fixture.worker.setInstanceId(instanceId);
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setFactoryName("test");
        task.setName("test-task");
        task.setStatus(TaskStatus.CANCELED);
        task.setAssignedWorker(fixture.worker);
        when(fixture.workers.findForUpdateById(fixture.session.getId())).thenReturn(Optional.of(fixture.worker));
        when(fixture.tasks.findById(task.getId())).thenReturn(Optional.of(task));

        WebException exception = assertThrows(WebException.class, () -> fixture.controller.reportSuccess(
                task.getId(),
                new TaskCompletionRequest(instanceId, "result", null),
                fixture.session
        ));

        assertEquals(HttpStatus.NOT_FOUND, exception.status);
        verify(fixture.taskService, never()).resolveSuccess(any(), any());
    }

    private static final class Fixture {

        private final WorkerRepository workers = mock(WorkerRepository.class);
        private final TaskRepository tasks = mock(TaskRepository.class);
        private final TaskService taskService = mock(TaskService.class);
        private final WorkerService workerService = mock(WorkerService.class);
        private final WorkerController controller = new WorkerController(workers, tasks, taskService, workerService);
        private final SessionToken session = session();
        private final Worker worker = worker(session);

        private static SessionToken session() {

            SessionToken session = new SessionToken();
            session.setId(UUID.randomUUID());
            return session;
        }

        private static Worker worker(SessionToken session) {

            Worker worker = new Worker();
            worker.setId(session.getId());
            worker.setSessionToken(session);
            worker.setLastPing(Instant.now());
            return worker;
        }
    }
}
