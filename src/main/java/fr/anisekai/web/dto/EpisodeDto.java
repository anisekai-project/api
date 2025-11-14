package fr.anisekai.web.dto;

import fr.anisekai.server.domain.entities.Episode;

public class EpisodeDto {

    public       Long id;
    public final long number;

    public EpisodeDto(Episode episode) {

        this.id     = episode.getId();
        this.number = episode.getNumber();
    }

    public String getName() {

        return String.format("Épisode %s", this.number);
    }

    public EpisodeDto(long number) {

        this.number = number;
    }

}
