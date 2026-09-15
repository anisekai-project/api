package fr.anisekai.library;

import fr.anisekai.ApplicationConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryPurgeTest {

    private static Library library(Path root) {

        ApplicationConfiguration config = new ApplicationConfiguration();
        config.getLibrary().setPath(root.toString());
        return new Library(config);
    }

    @Test
    void removesLeftoverStagingDirectoriesButKeepsRoot(@TempDir Path root) throws Exception {

        Library library = library(root);
        Path isolationRoot = root.resolve("isolation");
        Files.createDirectories(isolationRoot.resolve("stale-uuid/episodes"));
        Files.writeString(isolationRoot.resolve("stale-uuid/episodes/staged.mkv"), "stale");
        Files.createDirectories(isolationRoot.resolve("empty-husk"));

        library.purgeIsolationStagings();

        assertTrue(Files.isDirectory(isolationRoot));
        assertFalse(Files.exists(isolationRoot.resolve("stale-uuid")));
        assertFalse(Files.exists(isolationRoot.resolve("empty-husk")));
        library.close();
    }

    @Test
    void keepsEmptyIsolationRoot(@TempDir Path root) throws Exception {

        Library library = library(root);

        library.purgeIsolationStagings();

        assertTrue(Files.isDirectory(root.resolve("isolation")));
        library.close();
    }
}
