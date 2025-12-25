package fr.anisekai.core.persistence.annotations;


import fr.anisekai.core.persistence.events.EntityUpdatedEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TriggerEvent {

    Class<? extends EntityUpdatedEvent<?, ?>> value();

}
