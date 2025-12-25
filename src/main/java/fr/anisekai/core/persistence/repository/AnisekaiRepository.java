package fr.anisekai.core.persistence.repository;

import fr.anisekai.core.persistence.UpsertResult;
import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.exceptions.ForbiddenViolationException;
import fr.anisekai.core.persistence.exceptions.RequirementViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NoRepositoryBean
public interface AnisekaiRepository<T extends Entity<ID>, ID extends Serializable> extends
        ListCrudRepository<T, ID>,
        ListPagingAndSortingRepository<T, ID>,
        QueryByExampleExecutor<T> {

    /**
     * Retrieve an entity by ID, apply changes, and save it. Throws exception if entity is missing.
     *
     * @param id
     *         The {@code ID} to use with {@link #findById(Object)} to retrieve the entity.
     * @param updater
     *         The {@link Consumer} to use to update the entity state.
     *
     * @return The saved entity.
     */
    @NotNull T mod(@NotNull ID id, @NotNull Consumer<T> updater);

    /**
     * Upsert an entity into the database.
     *
     * @param retriever
     *         The {@link Supplier} to retrieve an optional entity.
     * @param initializer
     *         The {@link Supplier} to use to create the initial state if the entity was not found.
     * @param updater
     *         The {@link Consumer} to use to update the entity state.
     *
     * @return The inserted or updated entity.
     */
    @NotNull UpsertResult<T> upsert(@NotNull Supplier<Optional<T>> retriever, @NotNull Supplier<T> initializer, @NotNull Consumer<T> updater);

    /**
     * Upsert an entity into the database.
     *
     * @param id
     *         The {@code ID} to use with {@link #findById(Object)} to retrieve the entity.
     * @param initializer
     *         The {@link Supplier} to use to create the initial state if the entity was not found.
     * @param updater
     *         The {@link Consumer} to use to update the entity state.
     *
     * @return The inserted or updated entity.
     */
    default @NotNull UpsertResult<T> upsert(@NotNull ID id, @NotNull Supplier<T> initializer, @NotNull Consumer<T> updater) {

        return this.upsert(() -> this.findById(id), initializer, updater);
    }

    /**
     * Tries to retrieve an entity, and if no entity is found, a {@link RequirementViolationException} will be thrown.
     *
     * @param id
     *         The {@code ID} to use with {@link #findById(Object)} to retrieve the entity.
     *
     * @return The entity
     */
    default @NotNull T requireById(@NotNull ID id) {

        return this.require(() -> this.findById(id));
    }

    /**
     * Tries to retrieve an entity, and if no entity is found, a {@link RequirementViolationException} will be thrown.
     *
     * @param id
     *         The {@code ID} to use with {@link #findById(Object)} to retrieve the entity.
     *
     */
    default void forbidById(@NotNull ID id) {

        this.forbid(() -> this.findById(id));
    }

    /**
     * Tries to retrieve an entity, and if no entity is found, a {@link RequirementViolationException} will be thrown.
     *
     * @param retriever
     *         The {@link Supplier} to retrieve an optional entity.
     *
     * @return The entity
     */
    @NotNull T require(@NotNull Supplier<Optional<T>> retriever);

    /**
     * Tries to retrieve an entity, and if an entity is found, a {@link ForbiddenViolationException} will be thrown.
     *
     * @param retriever
     *         The {@link Supplier} to retrieve an optional entity.
     */
    void forbid(@NotNull Supplier<Optional<T>> retriever);

}
