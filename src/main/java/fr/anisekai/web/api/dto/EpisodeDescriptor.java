package fr.anisekai.web.api.dto;

import fr.anisekai.server.domain.entities.Episode;

import java.util.Collection;

public record EpisodeDescriptor(
        String mpd,
        String download,
        String anime,
        int number,
        Collection<TrackDto> tracks
) {

    public static EpisodeDescriptor of(Episode episode) {

        return new EpisodeDescriptor(
                "/api/v3/library/chunks/%d/meta.mpd".formatted(episode.getId()),
                "/api/v3/library/episodes/%d".formatted(episode.getId()),
                episode.getAnime().getTitle(),
                episode.getNumber(),
                episode.getTracks().stream().map(TrackDto::of).toList()
        );
    }

}
