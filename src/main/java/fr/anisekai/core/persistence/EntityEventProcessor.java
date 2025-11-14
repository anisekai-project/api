package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.events.EntityCreatedEvent;
import fr.anisekai.core.persistence.events.EntityDeletedEvent;
import fr.anisekai.core.persistence.events.EntityModifiedEvent;
import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.proxy.interfaces.State;
import fr.anisekai.proxy.reflection.Property;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

@Service
public class EntityEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEventProcessor.class);

    private final ApplicationEventPublisher publisher;

    public EntityEventProcessor(ApplicationEventPublisher publisher) {

        this.publisher = publisher;
    }

    public <T extends Entity<?>> void sendCreatedEvent(Object source, T entity) {

        this.publisher.publishEvent(new EntityCreatedEvent<>(source, entity));
    }

    public <T extends Entity<?>> void sendDeletedEvent(Object source, T entity) {

        this.publisher.publishEvent(new EntityDeletedEvent<>(source, entity));

    }

    public <T extends Entity<?>> void sendModifiedEvent(Object source, State<T> state, @Nullable T refreshedEntity) {

        if (!state.isDirty()) {
            return;
        }

        T eventTarget = Optional.ofNullable(refreshedEntity).orElse(state.getInstance());

        this.publisher.publishEvent(new EntityModifiedEvent<>(
                source,
                eventTarget,
                state.getOriginalState(),
                state.getDifferentialState()
        ));

        for (Map.Entry<Property, Object> entry : state.getDifferentialState().entrySet()) {
            createEntryEvent(entry, source, eventTarget).ifPresent(this.publisher::publishEvent);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity<?>> Optional<EntityPropertyChangedEvent<T, ?>> createEntryEvent(Map.Entry<Property, Object> entry, Object source, T instance) {

        Property     property     = entry.getKey();
        TriggerEvent triggerEvent = property.getField().getAnnotation(TriggerEvent.class);

        if (triggerEvent == null) {
            return Optional.empty();
        }

        Object oldValue = entry.getValue();
        Object currentValue;

        try {
            currentValue = property.getGetter().invoke(instance);
        } catch (InvocationTargetException | IllegalAccessException e) {
            LOGGER.warn(
                    "Ignoring trigger event on property {}: Could not invoke getter.",
                    property.getName(),
                    e
            );
            return Optional.empty();
        }

        Class<? extends EntityPropertyChangedEvent<?, ?>> eventClass = triggerEvent.value();

        Constructor<? extends EntityPropertyChangedEvent<?, ?>> constructor;

        try {
            constructor = eventClass.getConstructor(
                    Object.class,
                    instance.getClass(),
                    oldValue.getClass(),
                    currentValue.getClass()
            );
        } catch (NoSuchMethodException e) {
            LOGGER.warn(
                    "Ignoring trigger event on property {}: Could not find any constructor matching ({}, {}, {}, {}).",
                    property.getName(),
                    Object.class.getSimpleName(),
                    instance.getClass().getSimpleName(),
                    oldValue.getClass().getSimpleName(),
                    currentValue.getClass().getSimpleName()
            );
            return Optional.empty();
        }


        EntityPropertyChangedEvent<?, ?> event;

        try {
            return Optional.of((EntityPropertyChangedEvent<T, ?>) constructor.newInstance(
                    source,
                    instance,
                    oldValue,
                    currentValue
            ));
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.warn(
                    "Ignoring trigger event on property {}: Could not invoke constructor.",
                    property.getName(),
                    e
            );
            return Optional.empty();
        }
    }

}
