package fr.anisekai.web.enums;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Catalog of OAuth-style scopes granted to {@code APPLICATION} tokens.
 * <p>
 * The catalog intentionally covers <b>write</b> operations only. Read operations will be scoped later, when the
 * current permission system is replaced.
 * <p>
 * Scope names use the standard {@code scp} JWT claim (space-delimited) so a future OpenID-compliant authentication
 * solution can map upstream scopes to these values without changing route annotations.
 */
public final class TokenScope {

    /** Allows a worker to poll tasks, report results, and use task-scoped file endpoints. */
    public static final String WORKER = "worker";

    /** Allows anime imports (used by the browser extension). */
    public static final String ANIME_WRITE = "anime.write";

    /** All known scopes. */
    public static final Set<String> ALL = Set.of(WORKER, ANIME_WRITE);

    private TokenScope() {

    }

    public static boolean isKnown(String scope) {

        return scope != null && ALL.contains(scope);
    }

    /**
     * Validate requested scopes and return them deduplicated, dropping blanks and {@code null} entries.
     *
     * @param scopes
     *         The requested scopes, may be {@code null}.
     *
     * @return The validated scopes, never {@code null}.
     *
     * @throws IllegalArgumentException
     *         when any requested scope is unknown.
     */
    public static Set<String> validateAll(Collection<String> scopes) {

        if (scopes == null || scopes.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> validated = new LinkedHashSet<>();
        for (String scope : scopes) {
            if (scope == null || scope.isBlank()) {
                continue;
            }
            if (!isKnown(scope)) {
                throw new IllegalArgumentException("Unknown token scope: " + scope);
            }
            validated.add(scope);
        }
        return Collections.unmodifiableSet(validated);
    }

}
