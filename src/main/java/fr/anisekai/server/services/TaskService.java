package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.scheduler.commons.ActionPlan;
import fr.anisekai.scheduler.tasking.data.ReservedTaskMeta;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.data.TaskFailedPacket;
import fr.anisekai.scheduler.tasking.data.TaskMeta;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.scheduler.tasking.interfaces.factories.Factory;
import fr.anisekai.scheduler.tasking.interfaces.factories.FactoryRegistry;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.exceptions.task.TaskNotFoundException;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.tasking.server.ServerFactoryRegistry;
import fr.anisekai.server.tasking.server.ServerOrchestrator;
import fr.anisekai.utils.DataUtils;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TaskService extends AnisekaiService<Task, UUID, TaskRepository> {

    private final ServerOrchestrator serverOrchestrator;
    private final ServerFactoryRegistry serverFactory;
    private final DatabaseLockService  databaseLockService;

    public TaskService(
            TaskRepository repository,
            EntityEventProcessor eventProcessor,
            ServerOrchestrator serverOrchestrator,
            ServerFactoryRegistry serverFactory,
            DatabaseLockService databaseLockService
    ) {

        super(repository, eventProcessor);
        this.serverOrchestrator = serverOrchestrator;
        this.serverFactory      = serverFactory;
        this.databaseLockService = databaseLockService;
    }

    public boolean hasScheduled(String name) {

        return this.getRepository()
                   .existsByNameAndStatusIn(name, Arrays.asList(TaskStatus.SCHEDULED, TaskStatus.EXECUTING));
    }

    /**
     * Cancel all the scheduled {@link Task} matching the provided name.
     *
     * @param name
     *         The name of the {@link Task}s to cancel.
     */
    @Transactional
    public void cancel(String name) {

        List<Task> tasks = this.getRepository()
                               .findAllByNameAndStatusIn(name, Collections.singletonList(TaskStatus.SCHEDULED));

        for (Task task : tasks) {
            task.setStatus(TaskStatus.CANCELED);
        }
        this.getRepository().saveAll(tasks);
    }

    @Transactional
    public <F extends ServerFactory<Task, I, ?>, I> List<Task> queue(@NotNull Class<F> factoryClass, @NotNull Collection<I> arguments, byte priority) {

        this.databaseLockService.lock(DatabaseLockService.TASK_QUEUE);
        ActionPlan<UUID, ReservedTaskMeta, Task> plan = this.serverOrchestrator.queue(
                factoryClass,
                arguments,
                priority
        );
        return DataUtils.applyPlan(this.getRepository(), plan, this::createTask);
    }

    @Transactional
    public <F extends ServerFactory<Task, I, ?>, I> List<Task> queue(@NonNull F factory, @NotNull Collection<I> arguments, byte priority) {

        this.databaseLockService.lock(DatabaseLockService.TASK_QUEUE);
        ActionPlan<UUID, ReservedTaskMeta, Task> plan = this.serverOrchestrator.queue(factory, arguments, priority);
        return DataUtils.applyPlan(this.getRepository(), plan, this::createTask);
    }

    @Transactional
    public <F extends ServerFactory<Task, I, ?>, I> Task queueOne(@NotNull Class<F> factoryClass, @NotNull I argument, byte priority) {

        this.databaseLockService.lock(DatabaseLockService.TASK_QUEUE);
        F factory = this.serverFactory.query(factoryClass);
        String taskName = factory.getTaskName(argument);
        Optional<Task> active = this.getRepository().findFirstByFactoryNameAndNameAndStatusIn(
                factory.getName(),
                taskName,
                List.of(TaskStatus.SCHEDULED, TaskStatus.EXECUTING)
        );
        if (active.isPresent() && active.get().getStatus() == TaskStatus.EXECUTING) return active.get();

        List<Task> changed = this.queue(factory, List.of(argument), priority);
        if (!changed.isEmpty()) return changed.getFirst();

        return this.getRepository()
                   .findFirstByFactoryNameAndNameAndStatusIn(
                           factory.getName(),
                           taskName,
                           List.of(TaskStatus.SCHEDULED)
                   )
                   .orElseThrow(() -> new IllegalStateException("Scheduler returned an empty plan without an existing task"));
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public <R> List<Task> resolveSuccess(TaskMeta meta, String result) {

        Task          task         = this.requireByTaskMeta(meta);
        Factory<?, R> factory      = (Factory<?, R>) this.serverFactory.query(task.getFactoryName());
        R             resultObject = factory.getResultSerializer().deserialize(result);

        TaskExecutedPacket<Task, R>              packet = new TaskExecutedPacket<>(task, resultObject);
        ActionPlan<UUID, ReservedTaskMeta, Task> plan   = this.serverOrchestrator.resolve(packet);

        return DataUtils.applyPlan(this.getRepository(), plan, this::createTask);
    }

    @Transactional
    public List<Task> resolveFailure(TaskMeta meta, Throwable failure) {

        Task task = this.requireByTaskMeta(meta);

        Exception exception = failure instanceof Exception e ? e : new RuntimeException(failure);
        TaskFailedPacket<Task>                   packet = new TaskFailedPacket<>(task, exception);
        ActionPlan<UUID, ReservedTaskMeta, Task> plan   = this.serverOrchestrator.resolve(packet);

        return DataUtils.applyPlan(this.getRepository(), plan, this::createTask);
    }

    private Task createTask(ReservedTaskMeta data) {

        Task task = new Task();
        task.setFactoryName(data.factoryName());
        task.setName(data.name());
        task.setStatus(TaskStatus.SCHEDULED);
        task.setPriority(data.priority());
        task.setArguments(data.arguments());
        return task;
    }

    public Task requireByTaskMeta(TaskMeta meta) {

        return this.getRepository()
                   .findById(meta.identifier())
                   .orElseThrow(TaskNotFoundException::new);
    }

    public int recoverExecutingTasks() {

        return this.getRepository().resetExecuting(TaskStatus.EXECUTING, TaskStatus.SCHEDULED);
    }

}
