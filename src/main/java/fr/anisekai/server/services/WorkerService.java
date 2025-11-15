package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.repositories.WorkerRepository;
import fr.anisekai.web.exceptions.WebException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class WorkerService extends AnisekaiService<Worker, UUID, WorkerRepository> {

    public WorkerService(WorkerRepository repository, EntityEventProcessor eventProcessor) {

        super(repository, eventProcessor);
    }

    /**
     * Creates a new worker record and associates it with the authenticating session token.
     *
     * @param sessionToken
     *         The session token used by the worker to authenticate.
     * @param hostname
     *         An optional, user-provided hostname for observability.
     *
     * @return The newly created and persisted {@link Worker} entity.
     */
    public Worker registerWorker(SessionToken sessionToken, String hostname) {

        Worker worker = new Worker();
        worker.setSessionToken(sessionToken);
        worker.setHostname(hostname);
        worker.setLastHeartbeat(Instant.now());
        return this.getRepository().save(worker);
    }

    /**
     * Verifies that the worker identified by {@code workerId} is owned by the provided {@code sessionToken}.
     *
     * @param workerId
     *         The ID of the worker to verify.
     * @param sessionToken
     *         The session token claiming ownership.
     *
     * @return The {@link Worker} entity if ownership is verified.
     *
     * @throws fr.anisekai.web.exceptions.WebException
     *         if the worker is not found or not owned by the token.
     */
    public Worker requireWorkerOwnership(UUID workerId, SessionToken sessionToken) {

        return this.getRepository().findByIdAndSessionToken(workerId, sessionToken)
                   .orElseThrow(
                           () -> new WebException(
                                   HttpStatus.FORBIDDEN,
                                   "Worker ID does not belong to the authenticated session."
                           )
                   );
    }

}
