package fr.anisekai.core.persistence.events;


import fr.anisekai.core.persistence.domain.Entity;

public class EntityDeletedEvent<T extends Entity<?>> extends EntityEvent<T> {

    public EntityDeletedEvent(Object source, T entity) {

        super(source, entity);
    }

}
