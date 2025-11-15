package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.UuidEntity;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "worker")
public class Worker extends UuidEntity {

    @JoinColumn(name = "session_token_id")
    @ManyToOne(optional = false)
    private SessionToken sessionToken;

    @Column
    private String hostname;

    @Column(name = "last_heartbeat", nullable = false)
    private Instant lastHeartbeat;

    public @NotNull SessionToken getSessionToken() {

        return this.sessionToken;
    }

    public void setSessionToken(@NotNull SessionToken sessionToken) {

        this.sessionToken = sessionToken;
    }

    public String getHostname() {

        return this.hostname;
    }

    public void setHostname(String hostname) {

        this.hostname = hostname;
    }

    public @NotNull Instant getLastHeartbeat() {

        return this.lastHeartbeat;
    }

    public void setLastHeartbeat(@NotNull Instant lastHeartbeat) {

        this.lastHeartbeat = lastHeartbeat;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Worker worker) return EntityUtils.equals(this, worker);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
