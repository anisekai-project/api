package fr.anisekai.web.api.dto;

import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.media.enums.Disposition;
import fr.anisekai.server.domain.entities.Track;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record TrackDto(
        long id,
        String name,
        Codec codec,
        CodecType type,
        @Nullable String language,
        Collection<Disposition> dispositions
) {

    public static TrackDto of(Track track) {

        return new TrackDto(
                track.getId(),
                track.getName(),
                track.getCodec(),
                track.getCodec().getType(),
                track.getLanguage(),
                Disposition.fromBits(track.getDispositions())
        );
    }

}
