package fr.anisekai.web.api;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.api.responses.entities.AnimeResponse;
import fr.anisekai.web.enums.TokenType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v3/animes")
@Tag(name = "Animes", description = "Everything related to animes.")
public class AnimeController {

    private final AnimeService animeService;

    public AnimeController(AnimeService animeService) {

        this.animeService = animeService;
    }

    @Deprecated
    @PostMapping(value = "/import", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequireAuth(allowedSessionTypes = TokenType.APPLICATION)
    @Operation(summary = "[Deprecated] Import an anime", description = "This endpoint is used in the browser extension and should not be used for any other purpose.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import successful.")
    })
    public String importAnime(SessionToken session, @RequestBody String rawJson) {

        var result = this.animeService.importAnime(session.getOwner(), new AnisekaiJson(rawJson));

        return new AnisekaiJson()
                .putInTree("result.success", true)
                .putInTree("result.state", result.action().name())
                .toString();
    }

    @RequireAuth(allowGuests = false, requireAdmin = true)
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve all anime", description = "Allow to retrieve all animes from the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retrieval successful"),
            @ApiResponse(responseCode = "204", description = "Nothing to retrieve", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<List<AnimeResponse>> findAllAnime() {

        List<Anime> animes = this.animeService.getRepository().findAll();
        if (animes.isEmpty()) return ResponseEntity.noContent().build();

        List<AnimeResponse> data = animes
                .stream()
                .sorted(Comparator.comparing(Anime::getGroup).thenComparing(Anime::getOrder))
                .map(AnimeResponse::of)
                .toList();

        return ResponseEntity.ok(data);
    }

    @RequireAuth(allowGuests = false)
    @GetMapping(path = "/watchable", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve watchable anime", description = "Allow to retrieve all animes with at least one watchable episode.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retrieval successful"),
            @ApiResponse(responseCode = "204", description = "Nothing to retrieve", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<List<AnimeResponse>> findAllWatchableAnime() {

        List<Anime> animes = this.animeService.getRepository().findAllWatchable();
        if (animes.isEmpty()) return ResponseEntity.noContent().build();

        List<AnimeResponse> data = animes
                .stream()
                .sorted(Comparator.comparing(Anime::getGroup).thenComparing(Anime::getOrder))
                .map(AnimeResponse::of)
                .toList();

        return ResponseEntity.ok(data);
    }

}
