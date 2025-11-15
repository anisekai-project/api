package fr.anisekai.web.enums;

/**
 * Defines the scopes of an API token, determining which sets of endpoints it can access.
 */
public enum TokenScope {

    /**
     * Grants access to standard, user-facing API endpoints.
     */
    API,

    /**
     * Grants access to worker-specific endpoints for task processing.
     */
    WORKER

}
