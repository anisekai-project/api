package fr.anisekai.core.persistence.annotations;


import fr.anisekai.core.proxy.RepositoryProxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allowing to mark a repository method as one that should ignore proxying through {@link RepositoryProxy}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NoProxy {

}
