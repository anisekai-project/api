package fr.anisekai.web.enums;

import fr.anisekai.server.domain.entities.Anime;
import org.jetbrains.annotations.NotNull;

public enum AnimeStorageState {

    COMPLETE, INCOMPLETE, RELEASING;

    public static @NotNull AnimeStorageState of(Anime anime) {

        int total   = Math.abs(anime.getTotal());
        int current = anime.getEpisodes().size();

        boolean full = total == current;

        return switch (anime.getList()) {
            case SIMULCAST, SIMULCAST_AVAILABLE -> RELEASING;
            default -> full ? COMPLETE : INCOMPLETE;
        };
    }

}
