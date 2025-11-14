package fr.anisekai.web.dto.auth;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.AuthenticationManager;

public record AuthData(SessionToken accessToken, SessionToken refreshToken) {

    public AuthResponse toResponse(AuthenticationManager manager) {

        return new AuthResponse(manager.stringify(this.accessToken), manager.stringify(this.refreshToken));
    }

}
