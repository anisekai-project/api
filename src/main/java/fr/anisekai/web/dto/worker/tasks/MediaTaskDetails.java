package fr.anisekai.web.dto.worker.tasks;

import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.web.api.responses.entities.EpisodeDescriptorResponse;

public record MediaTaskDetails(
        EpisodeDescriptorResponse episode,
        ConversionSettings settings
) {

    public static MediaTaskDetails of(Episode episode, ConversionSettings settings) {

        return new MediaTaskDetails(EpisodeDescriptorResponse.of(episode), settings);
    }

}
