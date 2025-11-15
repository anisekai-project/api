package fr.anisekai.web.dto.worker.tasks;

import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.Disposition;
import fr.anisekai.server.domain.entities.Track;

import java.util.Set;

public record TrackCreationRequest(
        String name,
        Codec codec,
        String language,
        Set<Disposition> dispositions
) {

    public Track asTrack() {

        Track track = new Track();
        track.setName(this.name());
        track.setCodec(this.codec());
        track.setLanguage(this.language());
        track.setDispositions(Disposition.toBits(this.dispositions()));
        return track;
    }

}
