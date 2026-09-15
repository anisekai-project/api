package fr.anisekai.library;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.Sanctum;
import fr.anisekai.sanctum.SanctumUtils;
import fr.anisekai.sanctum.enums.StorePolicy;
import fr.anisekai.sanctum.exceptions.StorageException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;
import fr.anisekai.sanctum.stores.RawStorage;
import fr.anisekai.sanctum.stores.ScopedDirectoryStorage;
import fr.anisekai.sanctum.stores.ScopedFileStorage;
import fr.anisekai.server.domain.entities.*;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
public class Library extends Sanctum {

    private static final Logger LOGGER = LoggerFactory.getLogger(Library.class);

    public static final FileStore CHUNKS    = new ScopedDirectoryStorage("chunks", Episode.class);
    public static final FileStore EPISODES  = new ScopedFileStorage("episodes", Episode.class, "mkv");
    public static final FileStore SUBTITLES = new ScopedDirectoryStorage("subs", Episode.class);

    public static final FileStore EVENT_IMAGES = new ScopedFileStorage("event-images", Anime.class, "webp");

    public static final FileStore DOWNLOADS      = new RawStorage("downloads");
    public static final FileStore IMPORTS        = new RawStorage("imports");
    public static final FileStore LEGACY_EPISODE = new ScopedFileStorage("legacy", Episode.class, "mkv");

    private final ApplicationConfiguration.Library configuration;

    private final Map<SessionToken, List<UUID>> sessionIsolations = new HashMap<>();

    public Library(ApplicationConfiguration configuration) {

        super(configuration.getLibrary().getIoPath());
        this.configuration = configuration.getLibrary();

        this.registerStore(CHUNKS, StorePolicy.FULL_SWAP);
        this.registerStore(EPISODES, StorePolicy.OVERWRITE);
        this.registerStore(SUBTITLES, StorePolicy.FULL_SWAP);

        this.registerStore(EVENT_IMAGES, StorePolicy.OVERWRITE);

        this.registerStore(DOWNLOADS, StorePolicy.PRIVATE);
        this.registerStore(IMPORTS, StorePolicy.PRIVATE);
        this.registerStore(LEGACY_EPISODE, StorePolicy.PRIVATE);
    }

    public Optional<IsolationSession> resolveIsolationSession(SessionToken sessionToken, UUID uuid) {

        // Middleware
        List<UUID> allowed = this.sessionIsolations.getOrDefault(sessionToken, Collections.emptyList());

        if (allowed.contains(uuid)) {
            return Optional.of(this.getIsolatedStorage(uuid, true).context());
        }

        return Optional.empty();
    }

    public IsolationSession createIsolation(SessionToken sessionToken, AccessScope... scopes) {

        IsolationSession isolation = this.createIsolation(Set.of(scopes));
        this.sessionIsolations.computeIfAbsent(sessionToken, _ -> new ArrayList<>());
        this.sessionIsolations.get(sessionToken).add(isolation.uuid());
        return isolation;
    }

    /**
     * Best-effort discard of the provided isolation context. Discard failures are logged
     * and never propagated: task outcome takes precedence over staging cleanup.
     *
     * @param isolationId
     *         The identifier of the isolation context to discard.
     *
     * @return {@code true} when the context was discarded, {@code false} otherwise.
     */
    public boolean discardIsolation(UUID isolationId) {

        try {
            this.getIsolatedStorage(isolationId, true).context().close();
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to discard isolation context {}", isolationId, e);
            return false;
        }
        for (List<UUID> allowed : this.sessionIsolations.values()) {
            allowed.remove(isolationId);
        }
        return true;
    }

    public Path relativize(Path other) {

        return this.configuration.getIoPath().relativize(other);
    }

    /**
     * Delete every leftover isolation staging directory.
     * <p>
     * Sanctum forgets all isolation contexts on reboot, so library-assisted clearing is
     * impossible: leftovers are removed with plain IO instead. This is safe because committed
     * work already lives in the library while uncommitted work is discardable by definition
     * (no resume design exists), and fresh sessions mint new UUID directories that cannot
     * collide. Failures are logged and never propagated: boot must not hinge on staging cleanup.
     * <p>
     * Note: the {@code "isolation"} segment mirrors {@code Sanctum} internals; update together.
     */
    public void purgeIsolationStagings() {

        Path isolationRoot = this.configuration.getIoPath().resolve("isolation");
        if (!Files.isDirectory(isolationRoot)) return;

        List<Path> children;
        try (Stream<Path> stream = Files.list(isolationRoot)) {
            children = stream.toList();
        } catch (Exception e) {
            LOGGER.warn("Unable to list stale isolation stagings in {}", isolationRoot, e);
            return;
        }
        for (Path child : children) {
            try {
                SanctumUtils.delete(child);
            } catch (Exception e) {
                LOGGER.warn("Unable to purge stale isolation staging {}", child, e);
            }
        }
    }

    public Optional<Path> findDownload(TorrentFile torrentFile) {

        StorageResolver resolver = this.getResolver(DOWNLOADS);

        Path direct = resolver.file(torrentFile.getName());
        if (Files.isRegularFile(direct)) return Optional.of(direct);

        Torrent torrent   = torrentFile.getTorrent();
        Path    directory = resolver.directory(torrent.getName());
        if (!Files.isDirectory(directory)) return Optional.empty();

        Path target = directory.resolve(torrentFile.getName());
        if (Files.isRegularFile(target)) return Optional.of(target);
        return Optional.empty();
    }

    private List<String> getImportable(Predicate<Path> filter) {

        Path         directory = this.getResolver(IMPORTS).directory();
        List<String> content;

        try (Stream<Path> stream = Files.list(directory)) {
            content = stream
                    .filter(filter)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (Exception e) {
            throw new StorageException(e);
        }

        if (content.stream().anyMatch(name -> name.length() > 100)) {
            throw new StorageException("Some import names are over 100 characters long !");
        }

        return content;
    }

    public List<String> getImportableDirectories() {

        return this.getImportable(Files::isDirectory);
    }

    public List<String> getImportableFiles() {

        return this.getImportable(Files::isRegularFile);
    }

    @PreDestroy
    private void onClose() throws Exception {

        this.close();
    }

}
