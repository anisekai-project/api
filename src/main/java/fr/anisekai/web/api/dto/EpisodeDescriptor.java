package fr.anisekai.web.api.dto;

import fr.anisekai.server.domain.entities.Episode;

import java.util.Collection;

public record EpisodeDescriptor(
        long id,
        int number,
        String animeTitle,
        Collection<TrackDto> existingTracks
) {

    public static EpisodeDescriptor of(Episode episode) {

        return new EpisodeDescriptor(
                episode.getId(),
                episode.getNumber(),
                episode.getAnime().getTitle(),
                episode.getTracks().stream().map(TrackDto::of).toList()
        );
    }

}
