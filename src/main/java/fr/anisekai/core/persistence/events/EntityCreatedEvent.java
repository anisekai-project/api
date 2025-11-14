package fr.anisekai.core.persistence.events;


import fr.anisekai.core.persistence.domain.Entity;

public class EntityCreatedEvent<T extends Entity<?>> extends EntityEvent<T> {

    public EntityCreatedEvent(Object source, T entity) {

        super(source, entity);
    }

}
