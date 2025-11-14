package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.BaseEntity;
import fr.anisekai.server.domain.events.interest.InterestLevelUpdatedEvent;
import fr.anisekai.server.domain.keys.InterestKey;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Entity
@IdClass(InterestKey.class)
public class Interest extends BaseEntity<InterestKey> {

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private DiscordUser user;

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Anime anime;

    @Column(nullable = false)
    @TriggerEvent(InterestLevelUpdatedEvent.class)
    private byte level; // From -2 to 2 (0 being neutral)

    @Override
    public InterestKey getId() {

        return InterestKey.create(this.getAnime(), this.getUser());
    }

    public @NotNull DiscordUser getUser() {

        return this.user;
    }

    public void setUser(@NotNull DiscordUser user) {

        this.user = user;
    }

    public @NotNull Anime getAnime() {

        return this.anime;
    }

    public void setAnime(@NotNull Anime anime) {

        this.anime = anime;
    }

    public byte getLevel() {

        return this.level;
    }

    public void setLevel(byte level) {

        this.level = level;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Interest interest) return EntityUtils.equals(this, interest);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hash(this.getId());
    }

}
