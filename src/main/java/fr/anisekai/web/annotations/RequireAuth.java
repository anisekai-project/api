package fr.anisekai.web.annotations;

import fr.anisekai.web.enums.TokenScope;
import fr.anisekai.web.enums.TokenType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as requiring authentication, optionally enforcing additional access constraints.
 * <p>
 * This annotation supports fine-grained control over route-level authorization, including admin-only access, guest
 * allowance, and session type restrictions.
 * <p>
 * Authorization checks based on this annotation are typically enforced via a custom security interceptor or aspect. It
 * does not integrate with Spring Security by default.
 * <b>Limitations:</b> Session type filtering does not affect Spring's routing behavior —
 * you cannot define multiple identical endpoints with different {@link TokenType} constraints. If different behavior is
 * needed, implement manual branching in the controller.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {

    /**
     * Indicates whether the route requires administrator privileges.
     *
     * @return {@code true} if admin access is required, {@code false} otherwise.
     */
    boolean requireAdmin() default false;

    /**
     * Indicates whether the route allows guest users. A guest is a user who has not been granted full website access.
     *
     * @return {@code true} if guests are allowed, {@code false} otherwise.
     */
    boolean allowGuests() default true;

    /**
     * Specifies the allowed {@link TokenType}s for this route. This can be useful for restricting access to
     * application-only or OAuth-only sessions.
     * <p>
     * <b>Limitations:</b> Session type filtering does not affect Spring's routing behavior —
     * you cannot define multiple identical endpoints with different {@link TokenType} constraints. If different
     * behavior is needed, implement manual branching in the controller.
     *
     * @return The allowed session types for this route.
     */
    TokenType[] allowedSessionTypes() default {TokenType.USER, TokenType.APPLICATION};

    /**
     * Specifies the required {@link TokenScope}s for this route. The session token must possess ALL the specified
     * scopes to be granted access.
     *
     * @return The required scopes.
     */
    TokenScope[] scopes() default {};

}
