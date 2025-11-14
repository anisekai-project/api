package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import fr.anisekai.core.persistence.interfaces.CloseableContext;
import fr.anisekai.core.proxy.RepositoryProxy;
import fr.anisekai.web.exceptions.WebException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Service class allowing to declare common features across all entity services, avoiding boilerplate.
 *
 * @param <T>
 *         Type of the entity
 */
public abstract class AnisekaiService<T extends Entity<PK>, PK extends Serializable, R extends AnisekaiRepository<T, PK>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnisekaiService.class);

    private static class ContextHolder<R> {

        final R proxy;
        int referenceCount = 1;

        ContextHolder(R proxy) {this.proxy = proxy;}

        void increment() {

            this.referenceCount++;
        }

        void decrement() {

            this.referenceCount--;
        }

    }

    private final R                    repository;
    private final EntityEventProcessor eventProcessor;

    private final transient ThreadLocal<ContextHolder<R>> proxiedRepositoryHolder = new ThreadLocal<>();

    public AnisekaiService(R repository, EntityEventProcessor eventProcessor) {

        this.repository     = repository;
        this.eventProcessor = eventProcessor;
    }

    /**
     * Retrieve the repository used by this {@link AnisekaiService}.
     *
     * @return A repository.
     */
    public R getRepository() {

        ContextHolder<R> ctx = this.proxiedRepositoryHolder.get();

        if (ctx != null) {
            LOGGER.trace("Proxied 'getRepository()' call in {}", this.getClass().getSimpleName());
            return ctx.proxy;
        }

        LOGGER.debug("Unproxied 'getRepository()' call in {}", this.getClass().getSimpleName());
        return this.repository;
    }

    /**
     * Activates event support for the repository within a defined scope.
     * <p>
     * If a context is already active for the current thread, this method returns a no-op context, ensuring that only
     * the outermost call is responsible for cleanup. This prevents premature closing of the context in nested service
     * calls.
     *
     * @return A {@link CloseableContext} to be used in a try-with-resources statement.
     */
    public final CloseableContext withEventsSupport() {

        ContextHolder<R> holder = this.proxiedRepositoryHolder.get();

        if (holder != null) {
            holder.increment();
            return () -> {
                ContextHolder<R> currentHolder = this.proxiedRepositoryHolder.get();
                if (currentHolder != null) {
                    currentHolder.decrement();
                }
            };
        }

        R proxy = RepositoryProxy.createProxy(this.repository, this.eventProcessor);
        holder = new ContextHolder<>(proxy);
        this.proxiedRepositoryHolder.set(holder);
        LOGGER.debug("Created event context for resource {}", this.getClass().getSimpleName());

        return () -> {
            try {
                ContextHolder<R> finalHolder = this.proxiedRepositoryHolder.get();
                if (finalHolder == null) {
                    return;
                }

                finalHolder.decrement();

                if (finalHolder.referenceCount > 0) {
                    LOGGER.error(
                            "[!] Event context closed with {} unclosed nested scope(s). This indicates a missing 'try-with-resources' block in a nested service call, which could lead to memory leak issues.",
                            finalHolder.referenceCount
                    );
                }

                finalHolder.proxy.close();
                LOGGER.debug("Closed event context for resource {}", this.getClass().getSimpleName());
            } finally {
                this.proxiedRepositoryHolder.remove();
            }
        };
    }

    /**
     * Upsert an entity into the repository.
     * <p>
     * <ol>
     *     <li>Try to select the entity using the provided {@link Function}.</li>
     *     <li>If the entity was not found, the {@link Supplier} will be called to create the initial entity state.</li>
     *     <li>In any case, the {@link Consumer} will be called to apply the up-to-date values to the entity.</li>
     * </ol>
     *
     * @param retriever
     *         The {@link Function} to select an entity using the repository.
     * @param initializer
     *         The {@link Supplier} to use to create the initial state if the entity was not found.
     * @param updater
     *         The {@link Consumer} to use to update the entity state.
     *
     * @return The inserted or updated entity.
     */
    public UpsertResult<T> upsert(Function<R, Optional<T>> retriever, Supplier<T> initializer, Consumer<T> updater) {

        Optional<T>  optionalEntity = retriever.apply(this.repository);
        UpsertAction action         = optionalEntity.isEmpty() ? UpsertAction.INSERTED : UpsertAction.UPDATED;

        T entity = optionalEntity.orElseGet(initializer);
        updater.accept(entity);
        T saved = this.repository.save(entity);
        return new UpsertResult<>(saved, action);
    }

    /**
     * Require a result from an optional result using the provided {@link Function}.
     *
     * @param retriever
     *         A {@link Function} returning an optional result
     *
     * @return The entity.
     *
     * @throws WebException
     *         if the optional was empty
     */
    public T require(Function<R, Optional<T>> retriever) {

        return this.require(retriever, () -> new WebException(HttpStatus.NOT_FOUND, "No result"));
    }

    /**
     * Require a result from an optional result using the provided {@link Function}.
     *
     * @param retriever
     *         A {@link Function} returning an optional result
     * @param exceptionSupplier
     *         The {@link Supplier} to use to generate the {@link WebException} when the entity is not found.
     *
     * @return The entity.
     *
     * @throws WebException
     *         if the optional was empty.
     */
    public T require(Function<R, Optional<T>> retriever, Supplier<WebException> exceptionSupplier) {

        return retriever.apply(this.getRepository()).orElseThrow(exceptionSupplier);
    }

    /**
     * Exclude a result from an optional result using the provided {@link Function}.
     *
     * @param retriever
     *         A {@link Function} returning an optional result
     *
     * @throws WebException
     *         if a result is found.
     */
    public void exclude(Function<R, Optional<T>> retriever) {

        this.exclude(retriever, () -> new WebException(HttpStatus.CONFLICT, "Already exists."));
    }

    /**
     * Exclude a result from an optional result using the provided {@link Function}.
     *
     * @param retriever
     *         A {@link Function} returning an optional result
     * @param exceptionSupplier
     *         The {@link Supplier} to use to generate the {@link WebException} when the entity is found.
     *
     * @throws WebException
     *         if a result is found.
     */
    public void exclude(Function<R, Optional<T>> retriever, Supplier<WebException> exceptionSupplier) {

        retriever.apply(this.getRepository()).ifPresent(item -> {
            throw exceptionSupplier.get();
        });
    }

    /**
     * Require a result from an optional result using the provided {@code PK}
     *
     * @param id
     *         The {@code PK} identifying the entity to find.
     *
     * @return The entity
     *
     * @throws WebException
     *         if the optional was empty
     */
    public T requireById(PK id) {

        return this.require(repository -> repository.findById(id));
    }

    /**
     * Modify an {@link Entity} {@code T} by its {@code PK}.
     *
     * @param id
     *         The {@code PK} identifying the entity to find.
     * @param consumer
     *         The {@link Consumer} that will allow to update the {@link Entity}
     *
     * @return The saved, up-to-date {@link Entity}.
     */
    public T mod(PK id, Consumer<T> consumer) {

        T entity = this.requireById(id);
        consumer.accept(entity);
        return this.getRepository().save(entity);
    }

    /**
     * Create an {@link Entity} and return its saved state.
     *
     * @param creator
     *         The {@link Supplier} allowing to create a new {@link Entity} instance.
     * @param updater
     *         The {@link Consumer} allowing to update the new {@link Entity} instance.
     *
     * @return The inserted {@link Entity}.
     */
    public T create(Supplier<T> creator, Consumer<T> updater) {

        T entity = creator.get();
        updater.accept(entity);
        return this.getRepository().save(entity);
    }

}
