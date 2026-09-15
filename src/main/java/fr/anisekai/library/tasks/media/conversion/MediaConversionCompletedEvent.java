package fr.anisekai.library.tasks.media.conversion;

import java.util.Objects;
import java.util.UUID;

public record MediaConversionCompletedEvent(UUID episodeId) {

    public MediaConversionCompletedEvent {

        Objects.requireNonNull(episodeId, "episodeId");
    }

}
