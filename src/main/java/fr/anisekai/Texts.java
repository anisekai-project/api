package fr.anisekai;


import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.domain.enums.AnimeSeason;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary class holding some user-facing string values until proper i18n is coded.
 */
public final class Texts {

    public static final Map<AnimeList, String> ANIME_LIST_NAMES = new HashMap<>() {{
        this.put(AnimeList.WATCHED, "Visionné");
        this.put(AnimeList.WATCHING, "Visionnage en cours");
        this.put(AnimeList.SIMULCAST, "Visionnage en simulcast");
        this.put(AnimeList.SIMULCAST_AVAILABLE, "Simulcast disponible");
        this.put(AnimeList.DOWNLOADED, "Téléchargé");
        this.put(AnimeList.DOWNLOADING, "Téléchargement en cours");
        this.put(AnimeList.NOT_DOWNLOADED, "Non téléchargé");
        this.put(AnimeList.NO_SOURCE, "Aucune source trouvée");
        this.put(AnimeList.UNAVAILABLE, "Pas encore sorti");
        this.put(AnimeList.CANCELLED, "Abandonné");
    }};

    public static final Map<AnimeSeason, String> ANIME_SEASON_NAMES = new HashMap<>() {{
        this.put(AnimeSeason.WINTER, "Hivers");
        this.put(AnimeSeason.SPRING, "Printemps");
        this.put(AnimeSeason.SUMMER, "Été");
        this.put(AnimeSeason.AUTUMN, "Automne");
    }};

    public static String formatted(AnimeList animeList) {

        return String.format("%s %s", animeList.getIcon(), ANIME_LIST_NAMES.get(animeList));
    }

    public static String formatted(AnimeSeason animeSeason, int year) {

        return String.format("%s %s", ANIME_SEASON_NAMES.get(animeSeason), year);
    }

    private Texts() {}

}
