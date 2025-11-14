package fr.anisekai.utils;


import fr.anisekai.core.persistence.domain.Entity;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

/**
 * Utility methods for comparing and handling {@link Entity} objects.
 * <p>
 * Provides safe equality and comparison operations that account for entity persistence state.
 */
public final class EntityUtils {

    private EntityUtils() {}

    /**
     * Compares two entities for equality based on their identifiers.
     * <p>
     * If either entity is considered new (i.e., {@link Entity#isNew()} returns {@code true}), they are treated as not
     * equal.
     *
     * @param entity
     *         The first {@link Entity} to compare
     * @param other
     *         the second {@link Entity} to compare
     * @param <E>
     *         The type of the {@link Entity}
     * @param <PK>
     *         The type of the primary key
     *
     * @return {@code true} if both {@link Entity} objects are not new and have equal identifiers, {@code false}
     *         otherwise
     */
    public static <E extends Entity<PK>, PK extends Serializable> boolean equals(E entity, E other) {

        if (entity.isNew() || other.isNew()) {
            return false;
        }
        return Objects.equals(entity.getId(), other.getId());
    }

    /**
     * Compares two entities for equality using their identifiers, or a fallback value if either is new.
     * <p>
     * If either entity is considered new (i.e., {@link Entity#isNew()} returns {@code true}), the provided function is
     * used to compute a fallback value for comparison. Otherwise, the entities are compared based on their
     * identifiers.
     *
     * @param entity
     *         The first {@link Entity} to compare
     * @param other
     *         The second {@link Entity} to compare
     * @param externalCheck
     *         A function to extract a fallback comparison value for new entities
     * @param <E>
     *         The type of the {@link Entity}
     * @param <PK>
     *         The type of the primary key
     * @param <V>
     *         The type of the fallback comparison value
     *
     * @return {@code true} if the entities are equal based on their IDs or fallback values; {@code false} otherwise
     */
    public static <E extends Entity<PK>, PK extends Serializable, V> boolean equals(E entity, E other, Function<E, V> externalCheck) {

        if (entity.isNew() || other.isNew()) {
            return Objects.equals(externalCheck.apply(entity), externalCheck.apply(other));
        }
        return Objects.equals(entity.getId(), other.getId());
    }

    /**
     * Compares two entities based on their identifiers.
     * <p>
     * New entities (i.e., {@link Entity#isNew()} returns {@code true}) are considered greater than persisted ones. If
     * both are new, they are considered equal.
     *
     * @param entity
     *         The first {@link Entity} to compare
     * @param other
     *         The second {@link Entity} to compare
     * @param <E>
     *         The type of the {@link Entity}
     * @param <PK>
     *         The type of the primary key, which must be {@link Comparable}
     *
     * @return A negative integer, zero, or a positive integer if {@code entity} is less than, equal to, or greater than
     *         {@code other}
     */
    public static <E extends Entity<PK>, PK extends Serializable & Comparable<PK>> int compare(E entity, E other) {

        if (entity.isNew() && other.isNew()) {
            return 0;
        }
        if (entity.isNew() && !other.isNew()) {
            return 1;
        }
        if (!entity.isNew() && other.isNew()) {
            return -1;
        }
        return entity.getId().compareTo(other.getId());
    }

    /**
     * Compares two entities instances using a sequence of {@link Comparator}s.
     * <p>
     * The comparators are applied in order, and the first non-zero comparison result is returned. If all comparators
     * return zero, the entities are considered equal.
     *
     * @param entity
     *         The first {@link Entity} to compare
     * @param other
     *         The second {@link Entity} to compare
     * @param compares
     *         One or more {@link Comparator} instances used to determine ordering
     * @param <E>
     *         The type of the {@link Entity}
     * @param <PK>
     *         The type of the primary key, which must be {@link Comparable}
     *
     * @return A negative integer, zero, or a positive integer as {@code entity} is less than, equal to, or greater than
     *         {@code other}
     */
    @SafeVarargs
    public static <E extends Entity<PK>, PK extends Serializable & Comparable<PK>> int compare(E entity, E other, Comparator<E>... compares) {

        for (Comparator<E> compare : compares) {
            int comp = compare.compare(entity, other);
            if (comp != 0) {
                return comp;
            }
        }
        return 0;
    }

}
