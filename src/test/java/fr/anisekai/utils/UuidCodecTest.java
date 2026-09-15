package fr.anisekai.utils;

import fr.anisekai.discord.interactions.selection.SelectionButtonInteraction;
import fr.anisekai.discord.interactions.user.VoteButtonInteraction;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Selection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidCodecTest {

    @Test
    void roundTripsUuidWithoutPadding() {

        UUID uuid = UUID.fromString("018f73a4-5b6c-7def-8123-456789abcdef");

        String encoded = UuidCodec.encode(uuid);

        assertEquals(UuidCodec.ENCODED_LENGTH, encoded.length());
        assertFalse(encoded.contains("="));
        assertEquals(uuid, UuidCodec.decode(encoded));
    }

    @Test
    void rejectsNonCanonicalEncoding() {

        assertThrows(IllegalArgumentException.class, () -> UuidCodec.decode("AAAAAAAAAAAAAAAAAAAAAA=="));
        assertThrows(IllegalArgumentException.class, () -> UuidCodec.decode("not-a-uuid"));
    }

    @Test
    void selectionCustomIdsFitDiscordLimit() {

        Selection selection = new Selection();
        selection.setId(UUID.fromString("018f73a4-5b6c-7def-8123-456789abcdef"));
        Anime anime = new Anime();
        anime.setId(UUID.fromString("018f73a4-5b6d-7def-8123-456789abcdef"));

        String vote = VoteButtonInteraction.of(selection, anime);
        String close = SelectionButtonInteraction.of(selection);

        assertTrue(vote.length() <= 100, () -> "Vote custom ID is " + vote.length() + " characters");
        assertTrue(close.length() <= 100, () -> "Close custom ID is " + close.length() + " characters");
        assertEquals(selection.getId(), UuidCodec.decode(parameter(vote, "selection")));
        assertEquals(anime.getId(), UuidCodec.decode(parameter(vote, "anime")));
    }

    private static String parameter(String customId, String name) {

        for (String parameter : customId.substring(customId.indexOf('?') + 1).split("&")) {
            String[] entry = parameter.split("=", 2);
            if (entry[0].equals(name)) return entry[1];
        }
        throw new IllegalArgumentException("Missing parameter " + name);
    }

}
