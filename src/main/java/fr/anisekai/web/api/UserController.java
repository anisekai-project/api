package fr.anisekai.web.api;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.domain.entities.Worker;
import fr.anisekai.server.services.WorkerService;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.dto.auth.ApiKeyData;
import fr.anisekai.web.dto.auth.ApiKeyRequest;
import fr.anisekai.web.enums.TokenScope;
import fr.anisekai.web.exceptions.WebException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/users")
public class UserController {

    private final AuthenticationManager manager;
    private final WorkerService         workerService;

    public UserController(AuthenticationManager manager, WorkerService workerService) {

        this.manager       = manager;
        this.workerService = workerService;
    }

    @PostMapping(value = "/api-key", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(requireAdmin = true, allowGuests = false)
    @Transactional
    public ResponseEntity<ApiKeyData> obtainApiKey(SessionToken session, @RequestBody(required = false) ApiKeyRequest request) {

        List<String> scopes = request == null ? List.of() : request.scopes();

        SessionToken token;
        try {
            token = this.manager.createApplicationToken(
                    session.getOwner(),
                    Instant.now().plus(30, ChronoUnit.DAYS),
                    TokenScope.validateAll(scopes)
            );
        } catch (IllegalArgumentException e) {
            throw new WebException(HttpStatus.BAD_REQUEST, e.getMessage(), e.getMessage(), e);
        }

        UUID workerId = null;
        Set<String> granted = token.getScopes();
        if (granted != null && granted.contains(TokenScope.WORKER)) {
            Worker worker = this.workerService.provision(token);
            workerId = worker.getId();
        }

        return ResponseEntity.ok(new ApiKeyData(this.manager.stringify(token), workerId));
    }

}
