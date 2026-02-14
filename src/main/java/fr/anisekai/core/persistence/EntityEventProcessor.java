package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.events.EntityCreatedEvent;
import fr.anisekai.core.persistence.events.EntityDeletedEvent;
import fr.anisekai.core.persistence.events.EntityModifiedEvent;
import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import fr.anisekai.proxy.reflection.Property;
import fr.anisekai.utils.ReflectionUtils;
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

    public <T extends Entity<?>> void sendModifiedEvent(Object source, T entity, Map<Property, Object> originalState, Map<Property, Object> differentialState) {

        if (differentialState.isEmpty()) {
            return;
        }

        this.publisher.publishEvent(new EntityModifiedEvent<>(
                source,
                entity,
                originalState,
                differentialState
        ));

        for (Map.Entry<Property, Object> entry : differentialState.entrySet()) {
            createEntryEvent(entry, source, entity).ifPresent(this.publisher::publishEvent);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity<?>> Optional<EntityUpdatedEvent<T, ?>> createEntryEvent(Map.Entry<Property, Object> entry, Object source, T instance) {

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

        Class<? extends EntityUpdatedEvent<?, ?>> eventClass = triggerEvent.value();

        Optional<Constructor<? extends EntityUpdatedEvent<?, ?>>> optionalConstructor = ReflectionUtils
                .findAdequateConstructor(
                        eventClass,
                        Object.class,
                        instance.getClass(),
                        oldValue.getClass(),
                        currentValue.getClass()
                );

        if (optionalConstructor.isEmpty()) {
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

        Constructor<? extends EntityUpdatedEvent<?, ?>> constructor = optionalConstructor.get();
        LOGGER.debug("Triggering event {}", eventClass);

        try {
            return Optional.of((EntityUpdatedEvent<T, ?>) constructor.newInstance(
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
