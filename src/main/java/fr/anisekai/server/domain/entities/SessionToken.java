package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.UuidEntity;
import fr.anisekai.utils.EntityUtils;
import fr.anisekai.web.enums.TokenType;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
public class SessionToken extends UuidEntity {

    @ManyToOne(optional = false)
    private DiscordUser owner;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TokenType type;

    @Column(nullable = false)
    private ZonedDateTime expiresAt;

    @Column
    private ZonedDateTime revokedAt;

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

    public ZonedDateTime getExpiresAt() {

        return this.expiresAt;
    }

    public void setExpiresAt(ZonedDateTime expiresAt) {

        this.expiresAt = expiresAt;
    }

    public ZonedDateTime getRevokedAt() {

        return this.revokedAt;
    }

    public void setRevokedAt(ZonedDateTime revokedAt) {

        this.revokedAt = revokedAt;
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
