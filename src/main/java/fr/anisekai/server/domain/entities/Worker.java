package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.UuidEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "worker")
public class Worker extends UuidEntity {

    @Column(name = "last_ping", nullable = false)
    private Instant lastPing;

    @Column(name = "session_token_id", nullable = false)
    private UUID sessionTokenId;

    public Instant getLastPing() {
        return lastPing;
    }

    public void setLastPing(Instant lastPing) {
        this.lastPing = lastPing;
    }

    public UUID getSessionTokenId() {
        return sessionTokenId;
    }

    public void setSessionTokenId(UUID sessionTokenId) {
        this.sessionTokenId = sessionTokenId;
    }
}