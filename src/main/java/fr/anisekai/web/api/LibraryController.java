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
import fr.anisekai.web.enums.TokenType;
import fr.anisekai.web.exceptions.WebException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/library")
public class LibraryController {

    private static final MediaType DEFAULT = MediaType.APPLICATION_OCTET_STREAM;
    private static final MediaType MKV     = MediaType.parseMediaType("video/x-matroska");
    private static final MediaType DASH    = MediaType.parseMediaType("application/dash+xml");
    private static final MediaType WEBP    = MediaType.parseMediaType("image/webp");

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryController.class);

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

    @RequireAuth(allowGuests = false)
    @GetMapping("/chunks/{episodeId:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}}/{name}")
    public ResponseEntity<InputStreamResource> getChunkItem(@PathVariable UUID episodeId, @PathVariable String name) {

        Episode         episode  = this.episodeService.requireById(episodeId);
        StorageResolver resolver = this.library.getResolver(Library.CHUNKS);
        Path            path     = resolver.file(episode, name);

        return this.webFile.serve(path, path.getFileName().toString().endsWith(".mpd") ? DASH : DEFAULT);
    }

    @RequireAuth(allowGuests = false)
    @GetMapping("/episodes/{episodeId:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}}")
    public ResponseEntity<InputStreamResource> getEpisode(@PathVariable UUID episodeId) {

        Episode         episode  = this.episodeService.requireById(episodeId);
        Anime           anime    = episode.getAnime();
        StorageResolver resolver = this.library.getResolver(Library.EPISODES);
        String          filename = String.format("%s %02d.mkv", anime.getTitle(), episode.getNumber());
        Path            path     = resolver.file(episode);

        return this.webFile.serve(path, MKV, filename);
    }

    @PutMapping(value = "/episodes/{episodeId:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}}",
            consumes = {"video/x-matroska", "application/octet-stream"})
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION, scopes = TokenScope.WORKER)
    @RequireIsolation
    @Operation(summary = "Stage converted episode into isolation",
            description = "Streams a converted MKV into the isolation context staging area. Staged only — committed when the task succeeds.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Episode staged in isolation."),
            @ApiResponse(responseCode = "400", description = "Missing isolation scope, empty body, or unresolvable episode scope.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
            @ApiResponse(responseCode = "404", description = "Episode not found.", content = @Content(schema = @Schema(implementation = WebException.Dto.class))),
    })
    public ResponseEntity<?> stageEpisode(@PathVariable UUID episodeId, IsolationSession isolation,
                                          @RequestBody(required = false) InputStream body) {

        if (isolation == null) {
            throw new WebException(HttpStatus.BAD_REQUEST, "Isolation session not available");
        }

        Episode episode = this.episodeService.requireById(episodeId);

        AccessScope scope;
        try {
            scope = new AccessScope(Library.EPISODES, episode.getScopedName());
        } catch (RuntimeException e) {
            throw new WebException(HttpStatus.BAD_REQUEST, "Unable to resolve episode scope", e);
        }

        Path target;
        try {
            target = isolation.resolve(scope);
        } catch (ScopeForbiddenException e) {
            LOGGER.warn("Worker upload denied: isolation {} does not grant EPISODES scope for episode {}",
                    isolation.uuid(), episodeId);
            throw new WebException(HttpStatus.BAD_REQUEST,
                    "Isolation context does not grant EPISODES scope for this episode", e);
        } catch (RuntimeException e) {
            throw new WebException(HttpStatus.BAD_REQUEST, "Unable to resolve isolation staging path", e);
        }

        if (body == null) {
            throw new WebException(HttpStatus.BAD_REQUEST, "Upload body is required");
        }

        try {
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (InputStream in = body) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(target) == 0) {
                Files.deleteIfExists(target);
                throw new WebException(HttpStatus.BAD_REQUEST, "Upload body is empty");
            }
        } catch (WebException e) {
            throw e;
        } catch (IOException e) {
            throw new WebException(HttpStatus.UNPROCESSABLE_CONTENT, "Unable to stage uploaded episode", e);
        }

        return ResponseEntity.ok().build();
    }

    @RequireAuth(allowGuests = false)
    @GetMapping("/subtitles/{trackId:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}}")
    public ResponseEntity<InputStreamResource> getSubtitle(@PathVariable UUID trackId) {

        Track track = this.trackService.requireById(trackId);
        if (track.getCodec().getType() != CodecType.SUBTITLE) return ResponseEntity.badRequest().build();

        StorageResolver resolver = this.library.getResolver(Library.SUBTITLES);
        Path            path     = resolver.file(track.getEpisode(), track.asFilename());

        return this.webFile.serve(path, MediaType.parseMediaType(track.getCodec().getMimeType()), null);
    }

    @GetMapping("/event-images/{animeId:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}}")
    public ResponseEntity<InputStreamResource> getEventImage(@PathVariable UUID animeId) {

        Anime anime = this.animeService.requireById(animeId);

        StorageResolver resolver = this.library.getResolver(Library.EVENT_IMAGES);
        Path            path     = resolver.file(anime);

        if (!Files.exists(path)) {
            URI redirectUri = URI.create("/assets/images/unknown.webp");
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                                 .location(redirectUri)
                                 .build();
        }

        return this.webFile.serve(path, WEBP, path.getFileName().toString());
    }

    @RequireAuth(allowGuests = false)
    @GetMapping("/downloads/{torrentId:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}}/{file:[0-9]*}")
    public ResponseEntity<InputStreamResource> getDownloadItem(@PathVariable UUID torrentId, @PathVariable int file) {

        //TODO
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @RequireAuth(allowGuests = false)
    @GetMapping("/downloads/{torrentId:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}}/{file}")
    public ResponseEntity<InputStreamResource> getImportItem(@PathVariable UUID torrentId, @PathVariable int file) {

        //TODO
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @RequireAuth(allowGuests = false)
    @GetMapping("/downloads/{torrentId:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}}/{directory}/{file}")
    public ResponseEntity<InputStreamResource> getImportItem(@PathVariable UUID torrentId, @PathVariable String directory, @PathVariable String file) {

        //TODO
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }


}
