package fr.anisekai.web.dto.worker.tasks;

import java.util.List;

public record TaskCompletionRequest(
        List<TrackCreationRequest> newTracks
) {

}
