package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.exceptions.ForbiddenViolationException;
import fr.anisekai.core.persistence.exceptions.RequirementViolationException;
import fr.anisekai.core.persistence.interfaces.CloseableContext;
import fr.anisekai.core.persistence.interfaces.EventContextProvider;
import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.core.proxy.RepositoryProxy;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class AnisekaiService<T extends Entity<ID>, ID extends Serializable, R extends AnisekaiRepository<T, ID>> implements EventContextProvider {

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
            return ctx.proxy;
        }

        return this.repository;
    }

    @Override
    public CloseableContext startEventContext() {

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
     * Modify an {@link Entity} {@code T} by its {@code PK}.
     *
     * @param id
     *         The {@code PK} identifying the entity to find.
     * @param consumer
     *         The {@link Consumer} that will allow to update the {@link Entity}
     *
     * @return The saved, up-to-date {@link Entity}.
     */
    public T mod(ID id, Consumer<T> consumer) {

        T entity = this.requireById(id);
        consumer.accept(entity);
        return this.getRepository().save(entity);
    }

    /**
     * Upsert an entity into the database.
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

        Optional<T>  optionalEntity = retriever.apply(this.getRepository());
        UpsertAction action         = optionalEntity.isEmpty() ? UpsertAction.INSERTED : UpsertAction.UPDATED;

        T entity = optionalEntity.orElseGet(initializer);
        updater.accept(entity);
        T saved = this.getRepository().save(entity);
        return new UpsertResult<>(saved, action);
    }

    /**
     * Upsert an entity into the database.
     * <p>
     * <ol>
     *     <li>Try to select the entity using the provided primary key.</li>
     *     <li>If the entity was not found, the {@link Supplier} will be called to create the initial entity state.</li>
     *     <li>In any case, the {@link Consumer} will be called to apply the up-to-date values to the entity.</li>
     * </ol>
     *
     * @param key
     *         The primary key to use when retrieving the entity.
     * @param initializer
     *         The {@link Supplier} to use to create the initial state if the entity was not found.
     * @param updater
     *         The {@link Consumer} to use to update the entity state.
     *
     * @return The inserted or updated entity.
     */
    public @NotNull UpsertResult<T> upsert(@NotNull ID key, @NotNull Supplier<T> initializer, @NotNull Consumer<T> updater) {

        return this.upsert(repository -> repository.findById(key), initializer, updater);
    }

    /**
     * Method used to require the loading of an entity based on the provided primary key.
     *
     * @param key
     *         The primary key to use when retrieving the entity.
     *
     * @throws RequirementViolationException
     *         if the primary key doesn't match any saved entity.
     */
    public T requireById(@NotNull ID key) {

        return this.require(repository -> repository.findById(key));
    }

    /**
     * Method used to forbid the loading of an entity based on the provided primary key.
     *
     * @param key
     *         The primary key to use when retrieving the entity.
     *
     * @throws ForbiddenViolationException
     *         if the primary key matches a saved entity.
     */
    public void forbidById(@NotNull ID key) {

        this.forbid(repository -> repository.findById(key));
    }

    /**
     * Method used to require the loading of an entity based on the provided {@code retriever}.
     *
     * @param retriever
     *         A {@link Function} accepting the repository and returning an optional entity.
     *
     * @throws RequirementViolationException
     *         if the {@code retriever} returns an empty {@link Optional}.
     */
    public @NonNull T require(@NotNull Function<R, Optional<T>> retriever) {

        return retriever.apply(this.getRepository()).orElseThrow(RequirementViolationException::new);
    }

    /**
     * Method used to forbid the loading of an entity based on the provided {@code retriever}.
     *
     * @param retriever
     *         A {@link Function} accepting the repository and returning an optional entity.
     *
     * @throws ForbiddenViolationException
     *         if the {@code retriever} returns a non-empty {@link Optional}.
     */
    public void forbid(@NotNull Function<R, Optional<T>> retriever) {

        if (retriever.apply(this.getRepository()).isPresent()) {
            throw new ForbiddenViolationException();
        }
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
