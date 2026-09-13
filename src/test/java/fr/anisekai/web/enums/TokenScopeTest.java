package fr.anisekai.web.enums;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenScopeTest {

    @Test
    void acceptsKnownScopesAndDeduplicates() {

        assertEquals(
                Set.of(TokenScope.WORKER, TokenScope.ANIME_WRITE),
                TokenScope.validateAll(List.of(TokenScope.WORKER, TokenScope.ANIME_WRITE, TokenScope.WORKER))
        );
    }

    @Test
    void treatsMissingAndBlankScopesAsEmpty() {

        assertTrue(TokenScope.validateAll(null).isEmpty());
        assertTrue(TokenScope.validateAll(List.of()).isEmpty());
        assertTrue(TokenScope.validateAll(java.util.Arrays.asList(null, "  ")).isEmpty());
    }

    @Test
    void rejectsUnknownScopes() {

        assertThrows(IllegalArgumentException.class, () -> TokenScope.validateAll(List.of("anime.read")));
        assertThrows(IllegalArgumentException.class, () -> TokenScope.validateAll(List.of(TokenScope.WORKER, "admin")));
    }

    @Test
    void catalogOnlyCoversWriteOperations() {

        assertTrue(TokenScope.isKnown(TokenScope.WORKER));
        assertTrue(TokenScope.isKnown(TokenScope.ANIME_WRITE));
        assertFalse(TokenScope.isKnown("anime.read"));
        assertFalse(TokenScope.isKnown(null));
    }

}
