package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.library.Library;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.tasking.IsolatedServerFactory;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.scheduler.commons.ActionPlan;
import fr.anisekai.scheduler.tasking.data.ReservedTaskMeta;
import fr.anisekai.scheduler.tasking.data.TaskExecutedPacket;
import fr.anisekai.scheduler.tasking.data.TaskFailedPacket;
import fr.anisekai.scheduler.tasking.data.TaskMeta;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.scheduler.tasking.exceptions.UnknownFactoryException;
import fr.anisekai.scheduler.tasking.interfaces.factories.Factory;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskClient;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.exceptions.task.TaskNotFoundException;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.tasking.server.ServerFactoryRegistry;
import fr.anisekai.server.tasking.server.ServerOrchestrator;
import fr.anisekai.utils.DataUtils;
import fr.anisekai.web.exceptions.WebException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TaskService extends AnisekaiService<Task, UUID, TaskRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskService.class);

    private final ServerOrchestrator    serverOrchestrator;
    private final ServerFactoryRegistry serverFactory;
    private final DatabaseLockService   databaseLockService;
    private final Library               library;

    public TaskService(
            TaskRepository repository,
            EntityEventProcessor eventProcessor,
            ServerOrchestrator serverOrchestrator,
            ServerFactoryRegistry serverFactory,
            DatabaseLockService databaseLockService,
            Library library
    ) {

        super(repository, eventProcessor);
        this.serverOrchestrator  = serverOrchestrator;
        this.serverFactory       = serverFactory;
        this.databaseLockService = databaseLockService;
        this.library             = library;
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
        F      factory  = this.serverFactory.query(factoryClass);
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
                   .orElseThrow(() -> new IllegalStateException(
                           "Scheduler returned an empty plan without an existing task"));
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public <R> List<Task> resolveSuccess(TaskMeta meta, String result) {

        Task          task         = this.requireByTaskMeta(meta);
        Factory<?, R> factory      = (Factory<?, R>) this.serverFactory.query(task.getFactoryName());
        R             resultObject = factory.getResultSerializer().deserialize(result);

        if (task.getIsolationId() != null && !this.tryCommitTaskIsolation(task, factory)) {
            return this.resolveFailure(meta, new IllegalStateException(
                    "Isolation context of task " + task.getId() + " could not be committed"));
        }

        TaskExecutedPacket<Task, R>              packet = new TaskExecutedPacket<>(task, resultObject);
        ActionPlan<UUID, ReservedTaskMeta, Task> plan   = this.serverOrchestrator.resolve(packet);

        return DataUtils.applyPlan(this.getRepository(), plan, this::createTask);
    }

    @Transactional
    public List<Task> resolveFailure(TaskMeta meta, Throwable failure) {

        Task task = this.requireByTaskMeta(meta);
        this.discardTaskIsolation(task);

        Exception exception = failure instanceof Exception e ? e : new RuntimeException(
                failure);
        TaskFailedPacket<Task>                   packet = new TaskFailedPacket<>(task, exception);
        ActionPlan<UUID, ReservedTaskMeta, Task> plan   = this.serverOrchestrator.resolve(packet);

        return DataUtils.applyPlan(this.getRepository(), plan, this::createTask);
    }

    /**
     * Validate and commit the isolation context staged for the provided task.
     *
     * @param task
     *         The succeeding task.
     * @param factory
     *         The factory that produced the task.
     *
     * @return {@code true} when the isolation was committed, {@code false} when the
     *         task must go through failure handling instead.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean tryCommitTaskIsolation(Task task, Factory<?, ?> factory) {

        try {
            IsolationSession isolation = this.library.getIsolatedStorage(task.getIsolationId(), false).context();
            if (factory instanceof IsolatedServerFactory<?> isolated) {
                Object input = ((ServerFactory<Task, Object, ?>) factory).getArgumentsSerializer()
                                                                         .deserialize(task.getArguments());
                ((IsolatedServerFactory<Object>) isolated).validateStagedOutput(isolation, task, input);
            }
            isolation.commit();
            task.setIsolationId(null);
            return true;
        } catch (RuntimeException e) {
            LOGGER.warn("Isolation commit failed for task {}", task.getId(), e);
            this.discardTaskIsolation(task);
            return false;
        }
    }

    /**
     * Best-effort discard of the isolation context bound to the provided task.
     * Discard failures are logged and never propagated: task outcome takes precedence
     * over staging cleanup.
     *
     * @param task
     *         The task being resolved or released.
     */
    private void discardTaskIsolation(Task task) {

        if (task.getIsolationId() == null) return;
        this.library.discardIsolation(task.getIsolationId());
        task.setIsolationId(null);
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

    /**
     * Poll for a task on behalf of a worker, going through the {@link ServerOrchestrator} so factory attribution
     * callbacks ({@code onAssigningTask}) fire and claiming stays atomic.
     *
     * @param worker
     *         The worker polling for a task. It is recorded on the claimed task for audit and ownership checks.
     * @param factoryNames
     *         The compatible factory names declared by the worker. Must not be empty, all names must be known.
     *
     * @return The claimed task, if any.
     */
    @Transactional
    public Optional<Task> pollForWorker(@NotNull Worker worker, Collection<String> factoryNames) {

        if (factoryNames == null || factoryNames.isEmpty()) {
            throw new WebException(HttpStatus.BAD_REQUEST, "Worker must declare compatible factories");
        }

        List<ServerFactory<Task, ?, ?>> factories = new ArrayList<>(factoryNames.size());
        for (String factoryName : factoryNames) {
            try {
                factories.add(this.serverFactory.query(factoryName));
            } catch (UnknownFactoryException e) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Unknown factory: " + factoryName, e);
            }
        }

        TaskClient client = new TaskClient() {
            @Override
            public UUID getId() {

                return worker.getId();
            }

            @Override
            public @NotNull Collection<Factory<?, ?>> getSupportedFactories() {

                return List.copyOf(factories);
            }
        };

        Optional<Task> claimed = this.serverOrchestrator.poll(client);
        if (claimed.isPresent()) {
            Task task = claimed.get();
            task.setAssignedWorker(worker);
            ServerFactory<Task, ?, ?> factory = this.serverFactory.query(task.getFactoryName());
            if (factory instanceof IsolatedServerFactory<?> isolated) {
                this.createTaskIsolation(task, worker, factory, isolated);
            }
            this.getRepository().save(task);
        }
        return claimed;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void createTaskIsolation(Task task, Worker worker, ServerFactory<Task, ?, ?> factory, IsolatedServerFactory<?> isolated) {

        Object input = ((ServerFactory<Task, Object, ?>) factory).getArgumentsSerializer()
                                                                 .deserialize(task.getArguments());
        Set<AccessScope> scopes = ((IsolatedServerFactory<Object>) isolated).getIsolationScopes(task, input);
        if (scopes.isEmpty()) return;

        IsolationSession isolation = this.library.createIsolation(
                worker.getSessionToken(),
                scopes.toArray(new AccessScope[0])
        );
        task.setIsolationId(isolation.uuid());
    }

    public int recoverExecutingTasks() {

        return this.getRepository().resetExecuting(TaskStatus.EXECUTING, TaskStatus.SCHEDULED);
    }

}
