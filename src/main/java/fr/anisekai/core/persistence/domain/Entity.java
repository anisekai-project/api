package fr.anisekai.core.persistence.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * Interface representing a persistable entity.
 *
 * @param <PK>
 *         Type of the primary key.
 */
public interface Entity<PK extends Serializable> {

    /**
     * Retrieve this {@link Entity} primary key.
     *
     * @return The primary key.
     */
    PK getId();

    /**
     * Define this {@link Entity} primary key. Unless specifically overridden, this method will always throw an
     * exception as primary key as considered auto-incremented by default.
     *
     * @param id
     *         The primary key.
     *
     * @throws UnsupportedOperationException
     *         Threw if manual ID assignment is not supported on the current {@link Entity} type.
     */
    default void setId(PK id) {

        throw new UnsupportedOperationException("This entity does not support manual ID attribution.");
    }

    /**
     * Retrieve this {@link Entity} creation date.
     *
     * @return The creation date.
     */
    ZonedDateTime getCreatedAt();

    /**
     * Retrieve this {@link Entity} last update date.
     *
     * @return The last update date.
     */
    ZonedDateTime getUpdatedAt();

    /**
     * Check if this {@link Entity} has been persisted yet.
     *
     * @return True if not persisted, false otherwise.
     */
    default boolean isNew() {

        return this.getId() == null;
    }

}
