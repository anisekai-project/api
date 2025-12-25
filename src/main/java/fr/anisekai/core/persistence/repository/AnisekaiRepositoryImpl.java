package fr.anisekai.core.persistence.repository;

import fr.anisekai.core.persistence.UpsertAction;
import fr.anisekai.core.persistence.UpsertResult;
import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.exceptions.ForbiddenViolationException;
import fr.anisekai.core.persistence.exceptions.RequirementViolationException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AnisekaiRepositoryImpl<T extends Entity<ID>, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements AnisekaiRepository<T, ID> {

    public AnisekaiRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {

        super(entityInformation, entityManager);
    }

    @Override
    @Transactional
    public @NonNull T mod(@NonNull ID id, @NotNull Consumer<T> updater) {

        T entity = this.requireById(id);
        updater.accept(entity);
        return this.save(entity);
    }

    @Override
    @Transactional
    public @NotNull UpsertResult<T> upsert(@NotNull Supplier<Optional<T>> retriever, @NotNull Supplier<T> initializer, @NotNull Consumer<T> updater) {

        Optional<T>  optionalEntity = retriever.get();
        UpsertAction action         = optionalEntity.isEmpty() ? UpsertAction.INSERTED : UpsertAction.UPDATED;

        T entity = optionalEntity.orElseGet(initializer);
        updater.accept(entity);
        T saved = this.save(entity);
        return new UpsertResult<>(saved, action);
    }

    @Override
    public @NonNull T require(@NotNull Supplier<Optional<T>> retriever) {

        return retriever.get().orElseThrow(RequirementViolationException::new);
    }

    @Override
    public void forbid(@NotNull Supplier<Optional<T>> retriever) {

        if (retriever.get().isPresent()) {
            throw new ForbiddenViolationException();
        }
    }

}
