package fr.anisekai.core.annotations;

import fr.anisekai.server.domain.entities.Task;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allowing to tag any {@link Exception} that should make any {@link Task} immediately fails ignoring
 * {@link Task#getFailureCount} as it is not a temporary error.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FatalTask {

}
