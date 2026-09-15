package fr.anisekai.server.repositories;

import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.web.enums.TokenType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
class TaskResetTest {

    @Autowired
    private TaskRepository tasks;

    @Autowired
    private TestEntityManager entities;

    @Test
    void resetExecutingClearsWorkerAndIsolationBindings() {

        DiscordUser owner = new DiscordUser();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setNickname("owner");
        owner.setGuest(false);
        this.entities.persist(owner);

        SessionToken token = new SessionToken();
        token.setId(UUID.randomUUID());
        token.setOwner(owner);
        token.setType(TokenType.APPLICATION);
        token.setExpiresAt(Instant.now().plusSeconds(60));
        this.entities.persist(token);

        Worker worker = new Worker();
        worker.setId(token.getId());
        worker.setSessionToken(token);
        worker.setLastPing(Instant.now());
        this.entities.persist(worker);

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setFactoryName("test");
        task.setName("test-task");
        task.setStatus(TaskStatus.EXECUTING);
        task.setStartedAt(Instant.now());
        task.setArguments("{}");
        task.setAssignedWorker(worker);
        task.setIsolationId(UUID.randomUUID());
        this.entities.persist(task);
        this.entities.flush();
        this.entities.clear();

        assertEquals(1, this.tasks.resetExecuting(TaskStatus.EXECUTING, TaskStatus.SCHEDULED));
        this.entities.clear();

        Task recovered = this.tasks.findById(task.getId()).orElseThrow();
        assertEquals(TaskStatus.SCHEDULED, recovered.getStatus());
        assertNull(recovered.getStartedAt());
        assertNull(recovered.getAssignedWorker());
        assertNull(recovered.getIsolationId());
    }
}
