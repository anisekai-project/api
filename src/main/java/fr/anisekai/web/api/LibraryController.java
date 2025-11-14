package fr.anisekai.web.api;

import fr.anisekai.library.Library;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.server.services.TrackService;
import fr.anisekai.web.WebFile;
import fr.anisekai.web.annotations.RequireAuth;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @RequireAuth(allowGuests = false)
    @GetMapping("/chunks/{episodeId:[0-9]+}/{name}")
    public ResponseEntity<InputStreamResource> getChunkItem(@PathVariable long episodeId, @PathVariable String name) {

        Episode         episode  = this.episodeService.requireById(episodeId);
        StorageResolver resolver = this.library.getResolver(Library.CHUNKS);
        Path            path     = resolver.file(episode, name);

        return this.webFile.serve(path, path.getFileName().toString().endsWith(".mpd") ? DASH : DEFAULT);
    }

    @RequireAuth(allowGuests = false)
    @GetMapping("/episodes/{episodeId:[0-9]+}")
    public ResponseEntity<InputStreamResource> getEpisode(@PathVariable long episodeId) {

        Episode         episode  = this.episodeService.requireById(episodeId);
        Anime           anime    = episode.getAnime();
        StorageResolver resolver = this.library.getResolver(Library.EPISODES);
        String          filename = String.format("%s %02d.mkv", anime.getTitle(), episode.getNumber());
        Path            path     = resolver.file(episode);

        return this.webFile.serve(path, MKV, filename);

    }

    @RequireAuth(allowGuests = false)
    @GetMapping("/subtitles/{trackId:[0-9]+}")
    public ResponseEntity<InputStreamResource> getSubtitle(@PathVariable long trackId) {

        Track track = this.trackService.requireById(trackId);
        if (track.getCodec().getType() != CodecType.SUBTITLE) return ResponseEntity.badRequest().build();

        StorageResolver resolver = this.library.getResolver(Library.SUBTITLES);
        Path            path     = resolver.file(track.getEpisode(), track.asFilename());

        return this.webFile.serve(path, MediaType.parseMediaType(track.getCodec().getMimeType()), null);
    }

    @GetMapping("/event-images/{animeId:[0-9]+}")
    public ResponseEntity<InputStreamResource> getEventImage(@PathVariable long animeId) {

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
    @GetMapping("/downloads/{torrent}/{file:[0-9]*}")
    public ResponseEntity<InputStreamResource> getDownloadItem(@PathVariable String torrent, @PathVariable int file) {

        //TODO
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

}
