package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.IncrementableEntity;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Entity
public class Track extends IncrementableEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Episode episode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public Codec codec;

    @Column
    private String language;

    @Column(nullable = false)
    private int dispositions;

    public @NotNull Episode getEpisode() {

        return this.episode;
    }

    public void setEpisode(@NotNull Episode episode) {

        this.episode = episode;
    }

    public @NotNull String getName() {

        return this.name;
    }

    public void setName(@NotNull String name) {

        this.name = name;
    }

    public @NotNull Codec getCodec() {

        return this.codec;
    }

    public void setCodec(@NotNull Codec codec) {

        this.codec = codec;
    }

    public @Nullable String getLanguage() {

        return this.language;
    }

    public void setLanguage(String language) {

        this.language = language;
    }

    public int getDispositions() {

        return this.dispositions;
    }

    public void setDispositions(int dispositions) {

        this.dispositions = dispositions;
    }

    public String asFilename() {

        return String.format(
                "%s.%s",
                this.getId(),
                this.getCodec().getExtension()
        );
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Track track) return EntityUtils.equals(this, track);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
