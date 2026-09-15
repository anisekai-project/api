package fr.anisekai.web.dto;

/**
 * Directive issued by the API in a {@link WorkerPingResponse} when the worker-reported
 * state does not match the server-side state.
 */
public enum WorkerDirective {

    /** Nothing to do, the worker state matches the server state. */
    NONE,

    /**
     * The worker reported a task the server does not hold for it. The worker must abandon
     * its local task (no isolation context exists server-side, so it could never complete)
     * and report no task on its next ping.
     */
    GIVE_UP_TASK
}
