package fr.anisekai.core.persistence.events;


import fr.anisekai.core.persistence.domain.Entity;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public class EntityUpdatedEvent<E extends Entity<?>, T> extends EntityEvent<E> implements ResolvableTypeProvider {

    private final T previous;
    private final T next;

    public EntityUpdatedEvent(Object source, E entity, T previous, T current) {

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

    @Override
    public @Nullable ResolvableType getResolvableType() {

        Class<?> clazz = this.getClass();
        if (clazz.getTypeParameters().length == 2) {
            return ResolvableType.forClassWithGenerics(
                    clazz,
                    ResolvableType.forInstance(this.getEntity()),
                    ResolvableType.forInstance(this.getPrevious() != null ? this.getPrevious() : this.getCurrent())
            );
        }
        // Fallback to the base class structure for concrete subclasses
        return ResolvableType.forClassWithGenerics(
                EntityUpdatedEvent.class,
                ResolvableType.forInstance(this.getEntity()),
                ResolvableType.forInstance(this.getPrevious() != null ? this.getPrevious() : this.getCurrent())
        );
    }

}
