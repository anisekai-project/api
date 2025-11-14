package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.domain.Entity;

public record UpsertResult<T extends Entity<?>>(T entity, UpsertAction action) {

}
