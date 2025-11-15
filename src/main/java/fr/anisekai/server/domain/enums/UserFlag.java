package fr.anisekai.server.domain.enums;

/**
 * Represents a set of flags that can be assigned to a user to define their roles and permissions. Each flag is
 * represented by a bit in a bitmask, allowing for efficient storage and querying.
 */
public enum UserFlag {

    /**
     * Grants administrator-level privileges across the application.
     */
    ADMINISTRATOR(1),

    /**
     * Marks the user as a regular, who doesn't have restricted access to certain features.
     */
    REGULAR(2),

    /**
     * Marks the user as a system user, typically for internal processes.
     */
    ACTIVE(4),

    /**
     * Designates the user as trusted, allowing them to generate API keys for workers.
     */
    TRUSTED(8);

    private final int value;

    UserFlag(int value) {

        this.value = value;
    }

    public int getValue() {

        return this.value;
    }
}
