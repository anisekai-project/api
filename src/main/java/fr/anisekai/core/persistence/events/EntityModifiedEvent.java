package fr.anisekai.core.persistence.events;


import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.proxy.reflection.Property;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import java.util.Map;

public class EntityModifiedEvent<T extends Entity<?>> extends EntityEvent<T> implements ResolvableTypeProvider {

    private final Map<Property, Object> original;
    private final Map<Property, Object> changes;

    public EntityModifiedEvent(Object source, T entity, Map<Property, Object> original, Map<Property, Object> changes) {

        super(source, entity);
        this.original = original;
        this.changes  = changes;
    }

    public Map<Property, Object> getOriginal() {

        return this.original;
    }

    public Map<Property, Object> getChanges() {

        return this.changes;
    }

    @Override
    public @Nullable ResolvableType getResolvableType() {

        return ResolvableType.forClassWithGenerics(
                this.getClass(),
                ResolvableType.forInstance(this.getEntity())
        );
    }

}
