package fr.anisekai.web.api;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.dto.auth.ApiKeyData;
import fr.anisekai.web.dto.auth.ApiKeyRequest;
import fr.anisekai.web.enums.TokenScope;
import fr.anisekai.web.exceptions.WebException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v3/users")
public class UserController {

    private final AuthenticationManager manager;

    public UserController(AuthenticationManager manager) {

        this.manager = manager;
    }

    @PostMapping(value = "/api-key", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(requireAdmin = true, allowGuests = false)
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

        return ResponseEntity.ok(new ApiKeyData(this.manager.stringify(token)));
    }

}
