package fr.anisekai.web.api;

import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.services.EpisodeService;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.api.dto.EpisodeDescriptor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3/episodes")
public class EpisodeController {

    private final EpisodeService service;

    public EpisodeController(EpisodeService service) {

        this.service = service;
    }

    @RequireAuth(allowGuests = false)
    @GetMapping(path = "/{episodeId:[0-9]+}/descriptor", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpisodeDescriptor> getEpisodeDescriptor(@PathVariable long episodeId) {

        Episode episode = this.service.requireById(episodeId);
        return ResponseEntity.ok(EpisodeDescriptor.of(episode));
    }

}
