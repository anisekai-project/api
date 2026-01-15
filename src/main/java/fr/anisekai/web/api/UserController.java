package fr.anisekai.web.api;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.dto.auth.ApiKeyData;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v3/users")
public class UserController {

    private final AuthenticationManager manager;

    public UserController(AuthenticationManager manager) {

        this.manager = manager;
    }

    @PostMapping(value = "/api-key", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(requireAdmin = true, allowGuests = false)
    public ResponseEntity<ApiKeyData> obtainApiKey(SessionToken session) {

        SessionToken token = this.manager.createApplicationToken(
                session.getOwner(),
                Instant.now().plus(30, ChronoUnit.DAYS)
        );

        return ResponseEntity.ok(new ApiKeyData(this.manager.stringify(token)));
    }

}
