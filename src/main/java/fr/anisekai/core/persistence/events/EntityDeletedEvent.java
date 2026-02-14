package fr.anisekai.core.persistence.events;


import fr.anisekai.core.persistence.domain.Entity;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public class EntityDeletedEvent<T extends Entity<?>> extends EntityEvent<T> implements ResolvableTypeProvider {

    public EntityDeletedEvent(Object source, T entity) {

        super(source, entity);
    }

    @Override
    public @Nullable ResolvableType getResolvableType() {

        return ResolvableType.forClassWithGenerics(
                this.getClass(),
                ResolvableType.forInstance(this.getEntity())
        );
    }

}
