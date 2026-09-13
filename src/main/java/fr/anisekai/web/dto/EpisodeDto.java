package fr.anisekai.web.dto;

import fr.anisekai.server.domain.entities.Episode;

import java.util.UUID;

public class EpisodeDto {

    public final long number;
    public       UUID id;

    public EpisodeDto(Episode episode) {

        this.id     = episode.getId();
        this.number = episode.getNumber();
    }

    public EpisodeDto(long number) {

        this.number = number;
    }

    public String getName() {

        return String.format("Épisode %s", this.number);
    }

}
