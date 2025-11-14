package fr.anisekai.core.persistence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity<PK extends Serializable> implements Entity<PK> {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private ZonedDateTime updatedAt;

    @Override
    public final ZonedDateTime getCreatedAt() {

        return this.createdAt;
    }

    @Override
    public final ZonedDateTime getUpdatedAt() {

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
