package fr.anisekai.library;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.Sanctum;
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
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
public class Library extends Sanctum {

    public static final FileStore CHUNKS    = new ScopedDirectoryStorage("chunks", Episode.class);
    public static final FileStore EPISODES  = new ScopedFileStorage("episodes", Episode.class, "mkv");
    public static final FileStore SUBTITLES = new ScopedDirectoryStorage("subs", Episode.class);

    public static final FileStore EVENT_IMAGES = new ScopedFileStorage("event-images", Anime.class, "webp");

    public static final FileStore DOWNLOADS = new RawStorage("downloads");
    public static final FileStore IMPORTS   = new RawStorage("imports");

    private final ApplicationConfiguration.Library configuration;

    private final Map<SessionToken, List<IsolationSession>> isolationMap = new HashMap<>();

    public Library(ApplicationConfiguration configuration) {

        super(configuration.getLibrary().getIoPath());
        this.configuration = configuration.getLibrary();

        this.registerStore(CHUNKS, StorePolicy.FULL_SWAP);
        this.registerStore(EPISODES, StorePolicy.OVERWRITE);
        this.registerStore(SUBTITLES, StorePolicy.FULL_SWAP);

        this.registerStore(EVENT_IMAGES, StorePolicy.OVERWRITE);

        this.registerStore(DOWNLOADS, StorePolicy.PRIVATE);
        this.registerStore(IMPORTS, StorePolicy.PRIVATE);
    }

    public Optional<IsolationSession> resolveIsolation(SessionToken sessionToken, String isolation) {

        if (!this.isolationMap.containsKey(sessionToken)) return Optional.empty();
        return this.isolationMap.get(sessionToken).stream()
                                .filter(item -> item.name().equals(isolation))
                                .findFirst();
    }

    public IsolationSession createIsolation(SessionToken sessionToken, AccessScope... scopes) {

        IsolationSession isolation = this.createIsolation(scopes);
        this.isolationMap.computeIfAbsent(sessionToken, item -> new ArrayList<>());
        this.isolationMap.get(sessionToken).add(isolation);
        return isolation;
    }


    public Path relativize(Path other) {

        return this.configuration.getIoPath().relativize(other);
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
