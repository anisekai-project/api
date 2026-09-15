package fr.anisekai.core.persistence.events;


import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.proxy.interfaces.State;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public class EntityCreatedEvent<T extends Entity<?>> extends EntityEvent<T> implements ResolvableTypeProvider {

    public EntityCreatedEvent(Object source, T entity) {

        super(source, entity);
    }

    @Override
    public @Nullable ResolvableType getResolvableType() {

        T entity = this.getEntity();

        if (entity instanceof State<?> state) {
            entity = (T) state.getInstance();
        }

        return ResolvableType.forClassWithGenerics(
                this.getClass(),
                ResolvableType.forInstance(entity)
        );
    }

}
