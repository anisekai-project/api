package fr.anisekai.server.services;

import fr.anisekai.core.annotations.FatalTask;
import fr.anisekai.core.internal.json.exceptions.JSONValidationException;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.library.Library;
import fr.anisekai.library.tasks.executors.MediaImportTask;
import fr.anisekai.library.tasks.factories.MediaImportFactory;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.domain.enums.TaskStatus;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.exceptions.task.FactoryAlreadyRegisteredException;
import fr.anisekai.server.exceptions.task.FactoryNotFoundException;
import fr.anisekai.server.repositories.TaskRepository;
import fr.anisekai.server.tasking.TaskBuilder;
import fr.anisekai.server.tasking.TaskExecutor;
import fr.anisekai.server.tasking.TaskFactory;
import fr.anisekai.web.dto.worker.tasks.ConversionSettings;
import fr.anisekai.web.dto.worker.tasks.MediaTaskDetails;
import fr.anisekai.web.dto.worker.tasks.TrackCreationRequest;
import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService extends AnisekaiService<Task, Long, TaskRepository> {

    private final static Logger LOGGER           = LoggerFactory.getLogger(TaskService.class);
    private final static int    MAX_TASK_FAILURE = 3;

    private final Map<TaskPipeline, Collection<TaskFactory<?>>> factoryPipelines = new HashMap<>();
    private final Library                                       library;
    private final EpisodeService episodeService;

    public TaskService(TaskRepository repository, EntityEventProcessor eventProcessor, Library library, EpisodeService episodeService) {

        super(repository     , eventProcessor);

        this.library        = library;
        this.episodeService = episodeService;
    }

    /**
     * Register the {@link TaskFactory} into this {@link TaskService}. {@link TaskPipeline}s.
     *
     * @param pipeline
     *         The {@link TaskPipeline} into which the {@link TaskFactory} will be registered.
     * @param factory
     *         The {@link TaskFactory} to register.
     */
    public void registerFactory(@NotNull TaskPipeline pipeline, @NotNull TaskFactory<?> factory) {

        if (!this.factoryPipelines.containsKey(pipeline)) {
            this.factoryPipelines.put(pipeline, new HashSet<>());
        }

        for (Map.Entry<TaskPipeline, Collection<TaskFactory<?>>> entry : this.factoryPipelines.entrySet()) {
            if (entry.getValue().contains(factory)) {
                throw new FactoryAlreadyRegisteredException(entry.getKey(), factory);
            }
        }

        this.factoryPipelines.get(pipeline).add(factory);
    }

    /**
     * Retrieve the {@link TaskFactory} of the provided class.
     *
     * @param factoryClass
     *         Class of the {@link TaskFactory}.
     * @param <T>
     *         Type of the {@link TaskFactory}
     *
     * @return A {@link TaskFactory} instance
     */
    public <T extends TaskFactory<?>> T getFactory(@NotNull Class<T> factoryClass) {

        return this
                .factoryPipelines
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(factoryClass::isInstance)
                .map(factoryClass::cast)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Tried to retrieve an unregistered factory " + factoryClass.getName()));
    }

    /**
     * Cancel all the scheduled {@link Task} matching the provided name.
     *
     * @param name
     *         The name of the {@link Task}s to cancel.
     */
    public void cancel(String name) {

        List<Task> tasks = this.getRepository().findAllByNameAndStatus(name, TaskStatus.SCHEDULED);
        for (Task task : tasks) {
            task.setStatus(TaskStatus.CANCELED);
        }
        this.getRepository().saveAll(tasks);
    }

    /**
     * Find a {@link Task} matching the provided name.
     *
     * @param name
     *         The name of the {@link Task}
     *
     * @return An optional {@link Task}.
     */
    public Optional<Task> find(String name) {

        return this.getRepository().findByNameAndStatusIn(name, List.of(TaskStatus.SCHEDULED));
    }

    /**
     * Check if any scheduled {@link Task} match the provided name.
     *
     * @param name
     *         The name of the {@link Task}
     *
     * @return True if a {@link Task} matching the name is scheduled, false otherwise.
     */
    public boolean has(String name) {

        return this.find(name).isPresent();
    }

    /**
     * Create a new {@link Task} and queue it.
     *
     * @param builder
     *         The {@link TaskBuilder} to use to create the {@link Task}.
     *
     * @return The queued {@link Task}, or {@code null} if nothing has been queued.
     */
    public Task queue(TaskBuilder builder) {

        boolean isFactoryRegistered = this.factoryPipelines
                .values()
                .stream()
                .anyMatch(pipeline -> pipeline.contains(builder.getFactory()));

        if (!isFactoryRegistered) { // Safeguard, just in case we forgot to call registerFactory()
            throw new IllegalStateException("Tried to register a task on a unregistered factory " + builder.getName());
        }

        // This allows any task to inherit their own priority on subtasks if necessary.
        builder.getArgs().put(TaskExecutor.OPTION_PRIORITY, builder.getPriority());

        if (!builder.getFactory().allowDuplicated()) {
            Optional<Task> optionalTask = this.find(builder.getName());
            if (optionalTask.isPresent()) {
                Task task = optionalTask.get();

                if (task.getPriority() >= builder.getPriority()) {
                    LOGGER.debug(
                            "Queuing of task '{}' dropped: The task already exists with a higher priority.",
                            builder.getName()
                    );
                    return task;
                }

                LOGGER.info(
                        "Updating task '{}' priority from {} to {}",
                        task.getName(),
                        task.getPriority(),
                        builder.getPriority()
                );

                task.setPriority(builder.getPriority());
                return this.getRepository().save(task);
            }
        }

        LOGGER.info("Queuing task '{}' with a priority of {}.", builder.getName(), builder.getPriority());
        LOGGER.debug(" :: Arguments = {}", builder.getArgs());

        return this.getRepository().save(builder.build());
    }

    @Scheduled(cron = "0 * * * * *")
    public void executeHeavy() {

        this.runPipeline(TaskPipeline.HEAVY);
    }

    @Scheduled(cron = "0/5 * * * * *")
    public void executeSoft() {

        this.runPipeline(TaskPipeline.SOFT);
    }

    @Scheduled(cron = "0/5 * * * * *")
    public void executeMessaging() {

        this.runPipeline(TaskPipeline.MESSAGING);
    }

    @PostConstruct
    private void controlData() {

        List<Task> tasks = this.getRepository().findAllByStatus(TaskStatus.EXECUTING);

        for (Task task : tasks) {
            LOGGER.warn("Task {} was still running when the application stopped.", task.getId());
            task.setStatus(TaskStatus.SCHEDULED);
        }

        this.getRepository().saveAll(tasks);
    }

    /**
     * Find the next {@link Task} to execute in the provided {@link TaskPipeline}.
     *
     * @param pipeline
     *         The {@link TaskPipeline} into which the next {@link Task} will be retrieved.
     *
     * @return An {@link Optional} {@link Task}.
     */
    private Optional<Task> findNextTask(TaskPipeline pipeline) {

        Collection<TaskFactory<?>> factories = this.factoryPipelines
                .getOrDefault(pipeline, Collections.emptyList())
                .stream()
                //region External Task
                .filter(factory -> !factory.isPublic())
                //endregion
                .toList();

        if (factories.isEmpty()) {
            return Optional.empty();
        }

        Collection<String> factoryNames = factories.stream().map(TaskFactory::getName).toList();

        return this.getRepository().findNextOf(TaskStatus.SCHEDULED, factoryNames);
    }

    /**
     * Retrieve the {@link TaskFactory} of the provided {@link Task}.
     *
     * @param task
     *         The {@link Task} for which the {@link TaskFactory} must be retrieved.
     *
     * @return A {@link TaskFactory}.
     */
    private @NotNull TaskFactory<?> getTaskFactory(Task task) {

        String factoryName = task.getFactoryName();

        return this.factoryPipelines
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(factory -> factory.getName().equals(factoryName))
                .findAny()
                .orElseThrow(() -> new FactoryNotFoundException(task));
    }

    /**
     * Run the next {@link Task} found within the provided {@link TaskPipeline}. The method will return early if no task
     * are waiting to be executed.
     *
     * @param pipeline
     *         The {@link TaskPipeline} from which the {@link Task} should be executed.
     */
    private void runPipeline(TaskPipeline pipeline) {

        Optional<Task> optionalTask = this.findNextTask(pipeline);
        if (optionalTask.isEmpty()) {
            return;
        }

        Task task = optionalTask.get();

        try (ITimedAction timer = ITimedAction.create()) {
            timer.open("task", task.getFactoryName(), "Execution of the task");

            this.flagExecuting(task);

            try {
                timer.action("prepare", "Perform basic task checks");
                TaskExecutor executor = this.getTaskFactory(task).create();
                executor.validateParams(task.getArguments());
                timer.endAction();

                timer.action("exec", "Run the queued task");
                LOGGER.debug("[{}] Executing task...", task.getName());
                executor.execute(timer, task.getArguments());
                LOGGER.debug("[{}] Done.", task.getName());
                timer.endAction();

                this.flagSuccessful(task);

            } catch (Exception e) {
                timer.action("failure", "Handle task execution failure");
                LOGGER.error("[{}] Execution failure.", task.getName(), e);
                if (this.isFatal(e)) {
                    this.flagImmediateFailure(task);
                } else {
                    this.flagFailure(task);
                }
                timer.endAction();

                Map<String, Object> context = new HashMap<>();
                context.put("id", task.getId());
                context.put("factory", task.getFactoryName());
                context.put("name", task.getName());
                context.put("params", task.getArguments().toString());

                Sentry.withScope(scope -> {
                    scope.setContexts("Task", context);
                    Sentry.captureException(e);
                });
            }
            timer.endAction();
        }

        this.getRepository().save(task);
    }

    private void flagExecuting(Task entity) {

        entity.setStatus(TaskStatus.EXECUTING);
        entity.setStartedAt(Instant.now());
        entity.setCompletedAt(null);
    }

    private void flagSuccessful(Task entity) {

        entity.setStatus(TaskStatus.SUCCEEDED);
        entity.setCompletedAt(Instant.now());
    }

    private void flagFailure(Task entity) {

        entity.setFailureCount((byte) (entity.getFailureCount() + 1));

        if (entity.getFailureCount() >= MAX_TASK_FAILURE) {
            entity.setStatus(TaskStatus.FAILED);
        } else {
            entity.setStatus(TaskStatus.SCHEDULED);
        }

        entity.setStartedAt(null);
        entity.setCompletedAt(null);

    }

    private void flagImmediateFailure(Task entity) {

        entity.setFailureCount((byte) (entity.getFailureCount() + 1));
        entity.setStatus(TaskStatus.FAILED);
        entity.setStartedAt(null);
        entity.setCompletedAt(null);
    }

    private boolean isFatal(Exception ex) {

        return ex instanceof JSONValidationException || ex.getClass().isAnnotationPresent(FatalTask.class);
    }


    //region External Task Management

    public MediaTaskDetails getMediaTaskDetails(Task task) {

        if (task.getFactoryName().equals(MediaImportFactory.NAME)) {
            long    episodeId = task.getArguments().getLong(MediaImportTask.OPTION_EPISODE);
            Episode episode   = this.episodeService.requireById(episodeId);

            ConversionSettings settings = new ConversionSettings(
                    Codec.H264,
                    Codec.AAC,
                    Codec.SSA
            ); // This could come from config
            return MediaTaskDetails.of(episode, settings);
        }
        return null;
    }


    /**
     * Finds and acquires the next available public task for a given worker. This method is transactional and ensures
     * that a worker can only have one active task at a time.
     *
     * @param worker
     *         The worker requesting a task.
     *
     * @return An {@link Optional} containing the acquired {@link Task}, or empty if no task is available or if the
     *         worker already has an active task.
     */
    @Transactional
    public synchronized Optional<Task> acquirePublicTask(Worker worker) {

        if (this.getRepository().findByWorkerAndStatus(worker, TaskStatus.EXECUTING).isPresent()) {
            LOGGER.warn("Worker {} requested a task but already has one executing.", worker.getId());
            return Optional.empty();
        }

        List<String> publicFactoryNames = this.factoryPipelines
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(TaskFactory::isPublic)
                .map(TaskFactory::getName)
                .collect(Collectors.toList());

        if (publicFactoryNames.isEmpty()) {
            return Optional.empty();
        }

        Optional<Task> optionalTask = this.getRepository().findNextOf(TaskStatus.SCHEDULED, publicFactoryNames);

        if (optionalTask.isPresent()) {
            Task           task    = optionalTask.get();
            TaskFactory<?> factory = this.getTaskFactory(task);

            Set<AccessScope> scopes = factory.getRequiredScopes(task);

            IsolationSession isolation = this.library.createIsolation(
                    worker.getSessionToken(),
                    scopes.toArray(new AccessScope[0])
            );

            task.setStatus(TaskStatus.EXECUTING);
            task.setWorker(worker);
            task.setStartedAt(Instant.now());
            task.setExpiresAt(Instant.now().plus(factory.getLeaseDuration()));
            task.setIsolationId(isolation.uuid());

            return Optional.of(this.getRepository().save(task));
        }

        return Optional.empty();
    }

    public void extendTaskLease(Task task) {

        TaskFactory<?> factory = this.getTaskFactory(task);
        task.setExpiresAt(Instant.now().plus(factory.getLeaseDuration()));
        this.getRepository().save(task);
    }

    @Transactional
    public void completeTask(Task task, Collection<TrackCreationRequest> newTracks) {

        long episodeId = task.getArguments().getLong(MediaImportTask.OPTION_EPISODE);
        this.episodeService.ready(episodeId, newTracks);

        this.library.resolveIsolation(task).ifPresent(isolation -> {
            isolation.commit();
            isolation.close();
        });

        task.setStatus(TaskStatus.SUCCEEDED);
        task.setCompletedAt(Instant.now());
        task.setWorker(null);
        task.setIsolationId(null);
        task.setExpiresAt(null);
        this.getRepository().save(task);
    }

    /**
     * Marks a task as failed and discards its associated isolation session. This method handles the retry logic.
     *
     * @param task
     *         The task that failed.
     */
    public void failTask(Task task) {

        if (task.getIsolationId() != null && task.getWorker() != null) {
            this.library.resolveIsolation(task.getWorker().getSessionToken(), task.getIsolationId())
                        .ifPresent(IsolationSession::close); // close() is discard()
        }

        this.flagFailure(task); // Uses the existing failure logic

        task.setWorker(null);
        task.setIsolationId(null);
        task.setExpiresAt(null);
        this.getRepository().save(task);
    }

    //endregion
}
