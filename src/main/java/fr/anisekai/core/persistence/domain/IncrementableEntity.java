package fr.anisekai.core.persistence.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @deprecated Here for backward compatibility. All entities should slowly transition to {@link UuidEntity}.
 */
@Deprecated
@MappedSuperclass
public class IncrementableEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public Long getId() {

        return this.id;
    }

    @Override
    public void setId(Long id) {

        throw new UnsupportedOperationException("This entity's identifier is auto incremented and cannot be set.");
    }

}
