package fr.anisekai.server.tasking.server;

import fr.anisekai.scheduler.tasking.AbstractServerOrchestrator;
import fr.anisekai.scheduler.tasking.data.TaskExecutionPacket;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.scheduler.tasking.interfaces.factories.Factory;
import fr.anisekai.scheduler.tasking.interfaces.factories.FactoryRegistry;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskClient;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.repositories.TaskRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ServerOrchestrator extends AbstractServerOrchestrator<Task> {

    private final TaskRepository                              repository;
    private final FactoryRegistry<ServerFactory<Task, ?, ?>>  registry;

    public ServerOrchestrator(FactoryRegistry<ServerFactory<Task, ?, ?>> registry, TaskRepository repository) {

        super(registry, 5);
        this.repository = repository;
        this.registry   = registry;
    }

    @Override
    public @NotNull List<Task> getTasks() {

        return this.repository.findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus.SCHEDULED);
    }

    /**
     * Attribution loop mirroring the library default, with one addition: mutually exclusive
     * media tasks for the same episode never execute in parallel. A {@code media:convert}
     * candidate is skipped while its {@code media:store} counterpart executes, and vice versa.
     * Skipped candidates stay {@code SCHEDULED} for a later poll. Any future isolated factory
     * sharing episode scopes joins the pair below.
     */
    @Override
    public @NotNull Optional<Task> poll(@NotNull TaskClient client) {

        List<String> supportedFactoryNames = client.getSupportedFactories()
                                                   .stream()
                                                   .map(Factory::getName)
                                                   .toList();

        for (Task task : this.getTasks()) {
            if (task.getStatus() != TaskStatus.SCHEDULED) continue;
            if (!supportedFactoryNames.contains(task.getFactoryName())) continue;
            if (this.isMutuallyBlocked(task)) continue;
            if (!this.claim(task, client)) continue;

            ServerFactory<Task, ?, ?> factory = this.registry.query(task.getFactoryName());
            factory.onAssigningTask(new TaskExecutionPacket<>(task));
            return Optional.of(task);
        }

        return Optional.empty();
    }

    private boolean isMutuallyBlocked(@NotNull Task task) {

        String counterpart = switch (task.getFactoryName()) {
            case "media:convert" -> "media:store";
            case "media:store" -> "media:convert";
            default -> null;
        };
        if (counterpart == null) return false;

        String prefix = task.getFactoryName() + ":";
        if (!task.getName().startsWith(prefix) || task.getName().length() <= prefix.length()) return false;
        String counterpartName = counterpart + ":" + task.getName().substring(prefix.length());

        return this.repository.findFirstByFactoryNameAndNameAndStatusIn(
                counterpart,
                counterpartName,
                List.of(TaskStatus.EXECUTING)
        ).isPresent();
    }

    @Override
    public boolean claim(@NotNull Task task, @NotNull TaskClient client) {

        Instant startedAt = Instant.now();
        int     updated   = this.repository.claim(task.getId(), TaskStatus.SCHEDULED, TaskStatus.EXECUTING, startedAt);
        if (updated == 0) return false;

        task.setStatus(TaskStatus.EXECUTING);
        task.setStartedAt(startedAt);
        task.setCompletedAt(null);
        return true;
    }

}
