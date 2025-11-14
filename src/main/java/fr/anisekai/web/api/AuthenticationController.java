package fr.anisekai.web.api;


import fr.alexpado.lib.rest.exceptions.RestException;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.dto.auth.AuthData;
import fr.anisekai.web.dto.auth.AuthRequest;
import fr.anisekai.web.dto.auth.AuthResponse;
import fr.anisekai.web.dto.auth.UserDto;
import fr.anisekai.web.exceptions.WebException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v3/auth")
public class AuthenticationController {

    private final static Logger                LOGGER = LoggerFactory.getLogger(AuthenticationController.class);
    private final        AuthenticationManager manager;

    public AuthenticationController(AuthenticationManager manager) {

        this.manager = manager;
    }

    @PostMapping(value = "/code", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Authenticate with Discord", description = "Use the code provided to try and authenticate with Discord servers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful.", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failure.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
            @ApiResponse(responseCode = "500", description = "Authentication error.", content = @Content(schema = @Schema(implementation = WebException.Dto.class)))
    })
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {

        try {
            AuthData authentication = this.manager.authenticate(request.key());
            return ResponseEntity.ok(authentication.toResponse(this.manager));
        } catch (Exception ex) {
            LOGGER.error("Unable to authenticate user", ex);
            if (ex instanceof RestException rex) {
                LOGGER.debug("Feedback: {}", new String(rex.getBody()));
                if (rex.getCode() >= 400 && rex.getCode() < 500) {

                    throw new WebException(
                            HttpStatus.BAD_GATEWAY,
                            "Discord Exchange Error: %s".formatted(new String(rex.getBody())),
                            "An error occured with discord",
                            rex
                    );
                }
            }

            throw new WebException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create user tokens",
                    "Internal Server Error",
                    ex
            );
        }
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Refresh tokens", description = "Obtain a new set of tokens using the refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exchange successful.", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Exchange failure.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
            @ApiResponse(responseCode = "500", description = "Exchange error.", content = @Content(schema = @Schema(implementation = WebException.Dto.class)))
    })
    public ResponseEntity<AuthResponse> exchange(@RequestBody AuthRequest request) {

        AuthData authentication = this.manager.exchange(request.key());
        return ResponseEntity.ok(authentication.toResponse(this.manager));
    }

    @RequireAuth
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve session data", description = "Retrieve the user data currently associated to the session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User data.", content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = WebException.Dto.class)))
    })
    public ResponseEntity<UserDto> checkSession(SessionToken session) {

        return ResponseEntity.ok(UserDto.of(session.getOwner()));
    }

}
