package fr.anisekai.web.dto.worker.tasks;

import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.web.api.dto.EpisodeDescriptor;

public record MediaTaskDetails(
        EpisodeDescriptor episode,
        ConversionSettings settings
) {

    public static MediaTaskDetails of(Episode episode, ConversionSettings settings) {

        return new MediaTaskDetails(EpisodeDescriptor.of(episode), settings);
    }

}
