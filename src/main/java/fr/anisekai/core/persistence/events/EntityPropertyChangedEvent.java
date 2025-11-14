package fr.anisekai.core.persistence.events;


import fr.anisekai.core.persistence.domain.Entity;

public class EntityPropertyChangedEvent<E extends Entity<?>, T> extends EntityEvent<E> {

    private final T previous;
    private final T next;

    public EntityPropertyChangedEvent(Object source, E entity, T previous, T current) {

        super(source, entity);
        this.previous = previous;
        this.next     = current;
    }

    public T getPrevious() {

        return this.previous;
    }

    public T getCurrent() {

        return this.next;
    }

}
