package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.web.dto.WorkerDirective;

import java.util.Optional;

/**
 * Outcome of reconciling the worker-reported state with the server-side state during a ping.
 *
 * @param assigned
 *         The freshly claimed task, if any. Only present when the worker reported no task
 *         and held none server-side.
 * @param directive
 *         The directive for the worker. Anything but {@link WorkerDirective#NONE} means
 *         no task was assigned by this ping.
 */
public record WorkerPollResult(
        Optional<Task> assigned,
        WorkerDirective directive
) {
}
