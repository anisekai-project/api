package fr.anisekai.core.persistence.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.util.UUID;

@MappedSuperclass
public class UuidEntity extends BaseEntity<UUID> {

    @Id
    @JdbcTypeCode(Types.BINARY)
    private UUID id;

    @Override
    public UUID getId() {

        return this.id;
    }

    @Override
    public void setId(UUID id) {

        this.id = id;
    }

    @PrePersist
    private void beforeInsert() {

        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }

}
