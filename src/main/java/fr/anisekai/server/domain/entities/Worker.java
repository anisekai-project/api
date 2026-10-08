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

    @Column(nullable = true)
    private String name;

    @Column(name = "instance_id", nullable = true)
    private UUID instanceId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_token_id", nullable = false)
    private SessionToken sessionToken;

    public Instant getLastPing() {
        return lastPing;
    }

    public void setLastPing(Instant lastPing) {
        this.lastPing = lastPing;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @Nullable UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(@Nullable UUID instanceId) {
        this.instanceId = instanceId;
    }

    public @NotNull SessionToken getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(@NotNull SessionToken sessionToken) {
        this.sessionToken = sessionToken;
    }
}
