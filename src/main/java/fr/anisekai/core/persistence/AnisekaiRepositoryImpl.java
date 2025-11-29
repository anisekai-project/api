package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;

public class AnisekaiRepositoryImpl<T extends Entity<ID>, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements AnisekaiRepository<T, ID> {

    public AnisekaiRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {

        super(entityInformation, entityManager);
    }

    @Override
    public void close() {
        //NOOP
    }

}
