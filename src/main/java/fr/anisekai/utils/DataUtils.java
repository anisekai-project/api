package fr.anisekai.utils;

import fr.anisekai.core.internal.json.AnisekaiArray;
import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.media.enums.Disposition;
import fr.anisekai.server.domain.entities.Episode;
import fr.anisekai.server.domain.entities.Track;
import org.jetbrains.annotations.NotNull;

public final class DataUtils {

    private DataUtils() {}

    public static @NotNull AnisekaiArray getTracksArray(Episode episode) {

        AnisekaiArray tracks = new AnisekaiArray();
        for (Track track : episode.getTracks()) {
            AnisekaiJson trackJson = new AnisekaiJson();
            trackJson.put("id", track.getId());
            trackJson.put("name", track.getName());
            trackJson.put("codec", track.getCodec().name().toLowerCase());
            trackJson.put("type", track.getCodec().getType().name().toLowerCase());
            trackJson.put("language", track.getLanguage());

            trackJson.put(
                    "dispositions",
                    Disposition.fromBits(track.getDispositions()).stream().map(Disposition::name).toList()
            );

            tracks.put(trackJson);
        }
        return tracks;
    }

}
