package fr.anisekai.web.api;

import fr.anisekai.library.Library;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.exceptions.scope.ScopeForbiddenException;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.web.WebFile;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.annotations.RequireIsolation;
import fr.anisekai.web.enums.TokenScope;
import fr.anisekai.web.exceptions.WebException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v3/library")
public class LibraryController {

    private static final MediaType DEFAULT = MediaType.APPLICATION_OCTET_STREAM;
    private static final MediaType MKV     = MediaType.parseMediaType("video/x-matroska");
    private static final MediaType DASH    = MediaType.parseMediaType("application/dash+xml");
    private static final MediaType WEBP    = MediaType.parseMediaType("image/webp");

    private final Library        library;
    private final WebFile        webFile;
    private final AnimeService   animeService;
    private final EpisodeService episodeService;
    private final TrackService   trackService;

    public LibraryController(Library library, WebFile webFile, AnimeService animeService, EpisodeService episodeService, TrackService trackService) {

        this.library        = library;
        this.webFile        = webFile;
        this.animeService   = animeService;
        this.episodeService = episodeService;
        this.trackService   = trackService;
    }

    //region Chunks

    @GetMapping("/chunks/{claim:\\d+}/{file}")
    @RequireAuth(allowGuests = false)
    public ResponseEntity<InputStreamResource> readChunk(@PathVariable long claim, @PathVariable String file) {

        Episode         episode  = this.episodeService.requireById(claim);
        StorageResolver resolver = this.library.getResolver(Library.CHUNKS);
        Path            path     = resolver.file(episode, file);
        MediaType       mimeType = path.getFileName().toString().endsWith(".mpd") ? DASH : DEFAULT;
        return this.webFile.serve(path, mimeType);
    }

    @RequireIsolation
    @RequireAuth(scopes = {TokenScope.WORKER})
    @PostMapping(value = "/chunks/{claim:\\d+}/{file}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> writeChunk(
            IsolationSession isolation,
            @PathVariable long claim,
            @PathVariable String file,
            @RequestParam("file") MultipartFile uploadedFile
    ) throws IOException {

        Episode     episode = this.episodeService.requireById(claim);
        AccessScope scope   = new AccessScope(Library.CHUNKS, episode);

        try {
            Path path = isolation.resolve(scope, file);
            Files.createDirectories(path.getParent());
            uploadedFile.transferTo(path);
        } catch (ScopeForbiddenException e) {
            throw new WebException(
                    HttpStatus.FORBIDDEN,
                    "The isolation context does not have the required scope to write to this resource."
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //endregion

    //region Subs

    @RequireAuth(allowGuests = false)
    @GetMapping("/subs/{claim:\\d+}")
    public ResponseEntity<InputStreamResource> readSubs(@PathVariable long claim) {

        Track track = this.trackService.requireById(claim);
        if (track.getCodec().getType() != CodecType.SUBTITLE) return ResponseEntity.badRequest().build();

        StorageResolver resolver = this.library.getResolver(Library.SUBTITLES);
        Path            path     = resolver.file(track.getEpisode(), track.asFilename());
        MediaType       mimeType = MediaType.parseMediaType(track.getCodec().getMimeType());

        return this.webFile.serve(path, mimeType, null);
    }

    @RequireIsolation
    @RequireAuth(scopes = {TokenScope.WORKER})
    @PostMapping(value = "/subs/{claim:\\d+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> writeSubs(
            IsolationSession isolation,
            @PathVariable long claim,
            @RequestParam("file") MultipartFile uploadedFile
    ) throws IOException {

        Track track = this.trackService.requireById(claim);
        if (track.getCodec().getType() != CodecType.SUBTITLE) return ResponseEntity.badRequest().build();

        AccessScope scope = new AccessScope(Library.SUBTITLES, track.getEpisode());


        try {
            Path path = isolation.resolve(scope, track.asFilename());
            Files.createDirectories(path.getParent());
            uploadedFile.transferTo(path);
        } catch (ScopeForbiddenException e) {
            throw new WebException(
                    HttpStatus.FORBIDDEN,
                    "The isolation context does not have the required scope to write to this resource."
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //endregion

    //region Event Images

    @GetMapping("/event-images/{claim:\\d+}")
    public ResponseEntity<InputStreamResource> readEventImage(@PathVariable long claim) {

        Anime           anime    = this.animeService.requireById(claim);
        StorageResolver resolver = this.library.getResolver(Library.EVENT_IMAGES);
        Path            path     = resolver.file(anime);

        if (!Files.exists(path)) {
            URI redirectUri = URI.create("/assets/images/unknown.webp");
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(redirectUri).build();
        }

        return this.webFile.serve(path, WEBP);
    }

    @PostMapping(value = "/event-images/{claim:\\d+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireAuth(requireAdmin = true)
    public ResponseEntity<Void> writeEventImage(
            @PathVariable long claim,
            @RequestParam("file") MultipartFile uploadedFile
    ) throws IOException {

        Anime       anime = this.animeService.requireById(claim);
        AccessScope scope = new AccessScope(Library.EVENT_IMAGES, anime);

        try (IsolationSession isolation = this.library.createIsolation(scope)) {
            Path path = isolation.resolve(scope);
            uploadedFile.transferTo(path);

            isolation.commit();
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //endregion

    /**
     * @deprecated This endpoint will soon be removed in favor of "merging" MPD chunks on the fly instead of keeping the
     *         complete file on disk (storage space optimization)
     */
    @RequireAuth(allowGuests = false)
    @GetMapping("/episodes/{claim:\\d+}")
    @Deprecated
    public ResponseEntity<InputStreamResource> readEpisode(@PathVariable long claim) {

        Episode         episode  = this.episodeService.requireById(claim);
        Anime           anime    = episode.getAnime();
        StorageResolver resolver = this.library.getResolver(Library.EPISODES);
        String          filename = String.format("%s %02d.mkv", anime.getTitle(), episode.getNumber());
        Path            path     = resolver.file(episode);

        return this.webFile.serve(path, MKV, filename);

    }

}
