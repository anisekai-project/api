package fr.anisekai.web.dto.auth;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;
import fr.anisekai.web.api.responses.authentication.AuthenticationResponse;
import jakarta.validation.constraints.NotNull;

public record AuthData(@NotNull SessionToken accessToken, @NotNull SessionToken refreshToken) {

    public AuthenticationResponse toResponse(AuthenticationManager manager) {

        return new AuthenticationResponse(manager.stringify(this.accessToken), manager.stringify(this.refreshToken));
    }

}
