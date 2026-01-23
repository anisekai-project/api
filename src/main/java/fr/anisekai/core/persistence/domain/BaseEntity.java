package fr.anisekai.core.persistence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity<PK extends Serializable> implements Entity<PK> {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public Instant getCreatedAt() {

        return this.createdAt;
    }

    @Override
    public Instant getUpdatedAt() {

        return this.updatedAt;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        BaseEntity<?> that = (BaseEntity<?>) other;
        return Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
