package fr.anisekai.core.persistence.events;

import fr.anisekai.core.persistence.domain.Entity;
import org.springframework.context.ApplicationEvent;

public abstract class EntityEvent<T extends Entity<?>> extends ApplicationEvent {

    private final T entity;

    public EntityEvent(Object source, T entity) {

        super(source);
        this.entity = entity;
    }

    public T getEntity() {

        return this.entity;
    }

}
