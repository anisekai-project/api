package fr.anisekai.core.internal.plannifier.plan;

import fr.anisekai.core.internal.plannifier.interfaces.entities.Planifiable;

import java.io.Serializable;
import java.util.function.Consumer;

public sealed interface SchedulingAction<ID extends Serializable> {

    SchedulingActionType getActionType();

    /**
     * An instruction to create a new entity.
     *
     * @param data
     *         The data for the new entity.
     * @param <ID>
     *         The type of the entity's identifier.
     */
    record CreateAction<ID extends Serializable>(Planifiable<?> data) implements SchedulingAction<ID> {

        @Override
        public SchedulingActionType getActionType() {

            return SchedulingActionType.CREATE;
        }

    }

    /**
     * An instruction to update an existing entity.
     *
     * @param targetId
     *         The identifier of the entity to update.
     * @param updateHook
     *         A consumer containing the modifications to apply.
     * @param <ID>
     *         The type of the entity's identifier.
     */
    record UpdateAction<ID extends Serializable>(ID targetId, Consumer<Planifiable<?>> updateHook)
            implements SchedulingAction<ID> {

        @Override
        public SchedulingActionType getActionType() {

            return SchedulingActionType.UPDATE;
        }

    }

    /**
     * An instruction to delete an existing entity.
     *
     * @param targetId
     *         The identifier of the entity to delete.
     * @param <ID>
     *         The type of the entity's identifier.
     */
    record DeleteAction<ID extends Serializable>(ID targetId) implements SchedulingAction<ID> {

        @Override
        public SchedulingActionType getActionType() {

            return SchedulingActionType.DELETE;
        }

    }

}
