package fr.anisekai.library.exceptions;

import fr.anisekai.core.annotations.FatalTask;
import fr.anisekai.server.domain.entities.Track;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@FatalTask
public class IncompatibleTrackException extends RuntimeException {

    private final Track track;

    public IncompatibleTrackException(String message, Track track) {

        super(message);
        this.track = track;
    }

    public Track getTrack() {

        return this.track;
    }

}
