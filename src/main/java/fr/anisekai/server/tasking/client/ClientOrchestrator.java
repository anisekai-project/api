package fr.anisekai.server.tasking.client;

import fr.anisekai.scheduler.tasking.AbstractClientOrchestrator;
import fr.anisekai.scheduler.tasking.data.TaskMeta;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.Factory;
import fr.anisekai.scheduler.tasking.interfaces.factories.FactoryRegistry;
import fr.anisekai.scheduler.tasking.interfaces.orchestrator.ServerOrchestrator;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskClient;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.TaskService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientOrchestrator extends AbstractClientOrchestrator {

    private final ServerOrchestrator<Task>             orchestrator;
    private final FactoryRegistry<ClientFactory<?, ?>> registry;
    private final TaskService                          taskService;
    private final UUID                                 localClientId;
    private final TaskClient                           localClient;

    public ClientOrchestrator(@NotNull FactoryRegistry<ClientFactory<?, ?>> registry, ServerOrchestrator<Task> orchestrator, TaskService taskService) {

        super(registry);
        this.orchestrator  = orchestrator;
        this.registry      = registry;
        this.taskService   = taskService;
        this.localClientId = UUID.randomUUID();
        this.localClient   = new TaskClient() {
            @Override
            public UUID getId() {

                return ClientOrchestrator.this.localClientId;
            }

            @Override
            public @NotNull Collection<Factory<?, ?>> getSupportedFactories() {

                return new ArrayList<>(ClientOrchestrator.this.registry.getFactories());
            }
        };
    }

    @Override
    public Optional<TaskMeta> poll() {

        return this.orchestrator.poll(this.localClient).map(TaskMeta::of);
    }

    @Override
    public void onSuccess(@NotNull TaskMeta task, @NotNull String result) {

        this.taskService.resolveSuccess(task, result);
    }

    @Override
    public void onFailure(@NotNull TaskMeta task, @NotNull Throwable throwable) {

        this.taskService.resolveFailure(task, throwable);
    }

}
