package fr.anisekai.web.api;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.api.responses.authentication.ApiKeyResponse;
import fr.anisekai.web.api.responses.entities.UserResponse;
import fr.anisekai.web.enums.TokenType;
import fr.anisekai.web.exceptions.WebException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v3/users")
@Tag(name = "Users", description = "Everything related to users.")
public class UserController {

    private final AuthenticationManager manager;

    public UserController(AuthenticationManager manager) {

        this.manager = manager;
    }

    @PostMapping(value = "/api-key", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(requireAdmin = true, allowGuests = false, allowedSessionTypes = TokenType.USER)
    public ResponseEntity<ApiKeyResponse> obtainApiKey(SessionToken session) {

        SessionToken token = this.manager.createApplicationToken(
                session.getOwner(),
                Instant.now().plus(30, ChronoUnit.DAYS)
        );

        return ResponseEntity.ok(new ApiKeyResponse(this.manager.stringify(token)));
    }

    @RequireAuth
    @GetMapping(value = "/@me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve current user data", description = "Retrieve the user data currently associated to the session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User data.", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = WebException.Dto.class)))
    })
    public ResponseEntity<UserResponse> currentUser(SessionToken session) {

        return ResponseEntity.ok(UserResponse.of(session.getOwner()));
    }

}
