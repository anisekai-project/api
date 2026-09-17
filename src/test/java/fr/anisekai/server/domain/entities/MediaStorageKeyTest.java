package fr.anisekai.server.domain.entities;

import fr.anisekai.library.Library;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.sanctum.AccessScope;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaStorageKeyTest {

    private static final UUID UUID_KEY = UUID.fromString("018f73a4-5b6c-7def-8123-456789abcdef");

    @Test
    void newMediaEntitiesUseUuidStorageKeys() {

        Anime anime = new Anime();
        anime.setId(UUID_KEY);
        Episode episode = new Episode();
        episode.setId(UUID_KEY);
        Track track = track(UUID_KEY);

        assertEquals(UUID_KEY.toString(), anime.getScopedName());
        assertEquals(UUID_KEY.toString(), episode.getScopedName());
        assertEquals(UUID_KEY + "." + track.getCodec().getExtension(), track.asFilename());
    }

    @Test
    void migratedMediaEntitiesUseLegacyDiskStorageKeys() throws ReflectiveOperationException {

        Anime anime = new Anime();
        anime.setId(UUID_KEY);
        setDiskId(anime, 12L);
        Episode episode = new Episode();
        episode.setId(UUID_KEY);
        setDiskId(episode, 34L);
        Track track = track(UUID_KEY);
        setDiskId(track, 56L);

        assertEquals("12", anime.getScopedName());
        assertEquals("34", episode.getScopedName());
        assertEquals("56." + track.getCodec().getExtension(), track.asFilename());
    }

    @Test
    void eventImageScopePreservesNewAndLegacyStorageKeys() throws ReflectiveOperationException {

        Anime anime = new Anime();
        anime.setId(UUID_KEY);

        AccessScope scope = new AccessScope(Library.EVENT_IMAGES, anime.getScopedName());
        assertEquals(Library.EVENT_IMAGES, scope.store());
        assertEquals(UUID_KEY.toString(), scope.claim());

        setDiskId(anime, 12L);
        AccessScope migratedScope = new AccessScope(Library.EVENT_IMAGES, anime.getScopedName());
        assertEquals(Library.EVENT_IMAGES, migratedScope.store());
        assertEquals("12", migratedScope.claim());
    }

    private static Track track(UUID id) {

        Track track = new Track();
        track.setId(id);
        track.setCodec(Codec.values()[0]);
        return track;
    }

    private static void setDiskId(Object entity, long id) throws ReflectiveOperationException {

        Field field = entity.getClass().getDeclaredField("diskId");
        field.setAccessible(true);
        field.set(entity, id);
    }

}
