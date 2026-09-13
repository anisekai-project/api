package fr.anisekai.server.tasking.server;

import fr.anisekai.scheduler.tasking.AbstractServerOrchestrator;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.scheduler.tasking.interfaces.factories.FactoryRegistry;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskClient;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.repositories.TaskRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ServerOrchestrator extends AbstractServerOrchestrator<Task> {

    private final TaskRepository repository;

    public ServerOrchestrator(FactoryRegistry<ServerFactory<Task, ?, ?>> registry, TaskRepository repository) {

        super(registry, 5);
        this.repository = repository;
    }

    @Override
    public @NotNull List<Task> getTasks() {

        return this.repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED);
    }

    @Override
    public boolean claim(@NotNull Task task, @NotNull TaskClient client) {

        Instant startedAt = Instant.now();
        int updated = this.repository.claim(task.getId(), TaskStatus.SCHEDULED, TaskStatus.EXECUTING, startedAt);
        if (updated == 0) return false;

        task.setStatus(TaskStatus.EXECUTING);
        task.setStartedAt(startedAt);
        task.setCompletedAt(null);
        return true;
    }

}
