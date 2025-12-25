package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.events.EntityCreatedEvent;
import fr.anisekai.core.persistence.events.EntityDeletedEvent;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;

@Component
public class EntityEventProcessor implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEventProcessor.class);

    private final TriggerEventCache cache;
    private final ApplicationEventPublisher publisher;

    public EntityEventProcessor(TriggerEventCache cache, ApplicationEventPublisher publisher) {

        this.cache = cache;
        this.publisher = publisher;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {

        if (event.getEntity() instanceof Entity<?> entity) {
            this.publisher.publishEvent(new EntityCreatedEvent<>(this, entity));
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {

        if (event.getEntity() instanceof Entity<?> entity) {
            this.publisher.publishEvent(new EntityDeletedEvent<>(this, entity));
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {

        Object entity = event.getEntity();

        // Only process our base Entity types
        if (!(entity instanceof Entity)) return;

        EntityPersister persister       = event.getPersister();
        String[]        propertyNames   = persister.getPropertyNames();
        int[]           dirtyProperties = event.getDirtyProperties();
        Object[]        oldState        = event.getOldState();
        Object[]        newState        = event.getState();

        // If no properties changed, or oldState is unavailable (detached entities sometimes), skip.
        if (dirtyProperties == null || oldState == null) return;

        for (int index : dirtyProperties) {
            String propertyName = propertyNames[index];
            Object oldValue     = oldState[index];
            Object newValue     = newState[index];

            this.processPropertyChange(entity, propertyName, oldValue, newValue);
        }
    }

    private void processPropertyChange(Object entity, String propertyName, Object oldVal, Object newVal) {

        Constructor<?> constructor = this.cache.getEventConstructor(entity.getClass(), propertyName);
        if (constructor == null) return;

        try {
            // Arguments: Source (this), Entity, OldValue, NewValue
            Object event = constructor.newInstance(this, entity, oldVal, newVal);
            this.publisher.publishEvent(event);
        } catch (Exception e) {
            LOGGER.error("Unable to send EntityUpdatedEvent", e);
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {

        return false; // We want to fire immediately after SQL update, inside the transaction
    }

}
