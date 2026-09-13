package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.UuidEntity;
import fr.anisekai.utils.EntityUtils;
import fr.anisekai.web.enums.TokenType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "session_token")
public class SessionToken extends UuidEntity {

    @ManyToOne(optional = false)
    private DiscordUser owner;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_token_scope", joinColumns = @JoinColumn(name = "token_id"))
    @Column(name = "scope", nullable = false, length = 64)
    private Set<String> scopes = new HashSet<>();

    public DiscordUser getOwner() {

        return this.owner;
    }

    public void setOwner(DiscordUser owner) {

        this.owner = owner;
    }

    public TokenType getType() {

        return this.type;
    }

    public void setType(TokenType type) {

        this.type = type;
    }

    public Instant getExpiresAt() {

        return this.expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {

        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {

        return this.revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {

        this.revokedAt = revokedAt;
    }

    public Set<String> getScopes() {

        return this.scopes;
    }

    public void setScopes(Set<String> scopes) {

        this.scopes = scopes == null ? new HashSet<>() : new HashSet<>(scopes);
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof SessionToken session) return EntityUtils.equals(this, session);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
