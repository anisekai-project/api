package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.events.EntityUpdatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TriggerEventCache {

    // Cache Key: ClassName + PropertyName -> Event Constructor
    private final Map<Class<?>, Map<String, Constructor<?>>> cache = new ConcurrentHashMap<>();

    public Constructor<?> getEventConstructor(Class<?> entityClass, String propertyName) {

        return this.cache.computeIfAbsent(entityClass, this::scanEntity)
                         .get(propertyName);
    }

    private Map<String, Constructor<?>> scanEntity(Class<?> clazz) {

        Map<String, Constructor<?>> map = new HashMap<>();

        ReflectionUtils.doWithFields(
                clazz,
                field -> {
                    if (field.isAnnotationPresent(TriggerEvent.class)) {
                        TriggerEvent annotation = field.getAnnotation(TriggerEvent.class);

                        Class<? extends EntityUpdatedEvent<?, ?>> eventClass = annotation.value();

                        try {
                            // We expect a constructor: (Object source, Entity entity, PreviousType prev, CurrentType curr)
                            // Since type erasure makes generic lookup hard, we look for the standard 4-arg constructor via reflection logic
                            // or assume the standard signature matches Object/Entity/Object/Object.
                            Constructor<?>[] constructors = eventClass.getConstructors();
                            if (constructors.length > 0) {
                                map.put(field.getName(), constructors[0]);
                            }
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to inspect event class " + eventClass.getName(), e);
                        }
                    }
                }
        );
        return map;
    }

}
