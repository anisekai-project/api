package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.UuidEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Entity
@Table(name = "worker")
public class Worker extends UuidEntity {

    @Column(name = "last_ping", nullable = false)
    private Instant lastPing;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_token_id", nullable = false)
    private SessionToken sessionToken;

    public Instant getLastPing() {
        return lastPing;
    }

    public void setLastPing(Instant lastPing) {
        this.lastPing = lastPing;
    }

    public @NotNull SessionToken getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(@NotNull SessionToken sessionToken) {
        this.sessionToken = sessionToken;
    }
}