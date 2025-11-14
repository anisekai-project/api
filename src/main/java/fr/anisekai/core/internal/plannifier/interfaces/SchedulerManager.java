package fr.anisekai.core.internal.plannifier.interfaces;


import fr.anisekai.core.internal.plannifier.interfaces.entities.Planifiable;
import fr.anisekai.core.internal.plannifier.interfaces.entities.WatchTarget;

import java.util.List;
import java.util.function.Consumer;

/**
 * Defines the contract for handling persistence and updates of scheduled entities within a {@link Scheduler}.
 * <p>
 * This interface acts as a bridge between the scheduler logic and the underlying data store, allowing CRUD-like
 * operations to be abstracted and decoupled from the scheduling implementation.
 *
 * @param <T>
 *         The type of the {@link WatchTarget} associated with the planifiable elements.
 * @param <I>
 *         The planifiable type used for scheduling logic. Usually a base or mutable interface.
 * @param <E>
 *         The concrete persisted entity type, typically extending {@code I} and representing the database-aware version
 *         of the planifiable.
 */
public interface SchedulerManager<T extends WatchTarget, I extends Planifiable<T>, E extends I> {

    /**
     * Creates a new persisted entity based on the provided {@link Planifiable} data.
     *
     * @param planifiable
     *         The non-persisted planifiable data to create the entity from.
     *
     * @return The persisted entity instance representing the created schedule entry.
     */
    E create(Planifiable<T> planifiable);

    /**
     * Updates a single persisted entity using the provided update hook.
     * <p>
     * The update hook receives a mutable reference to the underlying planifiable data, which should be modified
     * in-place. The updated state is then persisted.
     *
     * @param entity
     *         The entity to be updated.
     * @param updateHook
     *         A function to mutate the planifiable portion of the entity.
     *
     * @return The updated entity after applying the modifications.
     */
    E update(E entity, Consumer<I> updateHook);

    /**
     * Updates multiple persisted entities in batch using the same update logic.
     * <p>
     * The update hook is applied to each individual entity in the collection, and all changes are persisted
     * accordingly.
     *
     * @param entities
     *         The list of entities to update.
     * @param updateHook
     *         A function to mutate each planifiable in the batch.
     *
     * @return A list of updated entities after all modifications have been applied.
     */
    List<E> updateAll(List<E> entities, Consumer<I> updateHook);

    /**
     * Deletes a scheduled entity from the underlying persistence layer.
     *
     * @param entity
     *         The entity to delete.
     *
     * @return True if the deletion was successful, false otherwise.
     */
    boolean delete(E entity);

}
