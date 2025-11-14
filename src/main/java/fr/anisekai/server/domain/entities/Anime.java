package fr.anisekai.server.domain.entities;

import fr.anisekai.core.internal.plannifier.interfaces.entities.WatchTarget;
import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.IncrementableEntity;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.server.domain.converters.PatternConverter;
import fr.anisekai.server.domain.converters.StringListConverter;
import fr.anisekai.server.domain.enums.AnimeList;
import fr.anisekai.server.domain.events.anime.*;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
public class Anime extends IncrementableEntity implements WatchTarget, ScopedEntity, Comparable<Anime> {

    @NotNull
    @Column(nullable = false)
    private String group;

    @Column(nullable = false)
    private byte order = 1;

    @NotNull
    @Column(nullable = false, unique = true)
    @TriggerEvent(AnimeTitleUpdatedEvent.class)
    private String title;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @TriggerEvent(AnimeListUpdatedEvent.class)
    private AnimeList list;

    @Nullable
    @Column(columnDefinition = "TEXT")
    @TriggerEvent(AnimeSynopsisUpdatedEvent.class)
    private String synopsis;

    @NotNull
    @Column
    @Convert(converter = StringListConverter.class)
    @TriggerEvent(AnimeTagsUpdatedEvent.class)
    private List<String> tags;

    @Nullable
    @Column
    @TriggerEvent(AnimeThumbnailUpdatedEvent.class)
    private String thumbnailUrl;

    @NotNull
    @Column(nullable = false, unique = true)
    @TriggerEvent(AnimeUrlUpdatedEvent.class)
    private String url;

    @Nullable
    @Column
    @Convert(converter = PatternConverter.class)
    private Pattern titleRegex;

    @Column
    @TriggerEvent(AnimeWatchedUpdatedEvent.class)
    private int watched = 0;

    @Column
    @TriggerEvent(AnimeTotalUpdatedEvent.class)
    private int total = 0;

    @Column
    private int episodeDuration = 0;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private DiscordUser addedBy;

    @Nullable
    @Column
    private Long anilistId;

    @Nullable
    @Column
    private Long announcementId;

    @NotNull
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "anime")
    private Set<Episode> episodes;

    public @NotNull String getGroup() {

        return this.group;
    }

    public void setGroup(@NotNull String group) {

        this.group = group;
    }

    public byte getOrder() {

        return this.order;
    }

    public void setOrder(byte order) {

        this.order = order;
    }

    public @NotNull String getTitle() {

        return this.title;
    }

    public void setTitle(@NotNull String title) {

        this.title = title;
    }

    public @NotNull AnimeList getList() {

        return this.list;
    }

    public void setList(@NotNull AnimeList list) {

        this.list = list;
    }

    public @Nullable String getSynopsis() {

        return this.synopsis;
    }

    public void setSynopsis(@Nullable String synopsis) {

        this.synopsis = synopsis;
    }

    public @NotNull List<String> getTags() {

        return this.tags;
    }

    public void setTags(@NotNull List<String> tags) {

        this.tags = tags;
    }

    public @Nullable String getThumbnailUrl() {

        return this.thumbnailUrl;
    }

    public void setThumbnailUrl(@Nullable String thumbnailUrl) {

        this.thumbnailUrl = thumbnailUrl;
    }

    public @NotNull String getUrl() {

        return this.url;
    }

    public void setUrl(@NotNull String url) {

        this.url = url;
    }

    public @Nullable Pattern getTitleRegex() {

        return this.titleRegex;
    }

    public void setTitleRegex(@Nullable Pattern titleRegex) {

        this.titleRegex = titleRegex;
    }

    public int getWatched() {

        return this.watched;
    }

    public void setWatched(int watched) {

        this.watched = watched;
    }

    public int getTotal() {

        return this.total;
    }

    public void setTotal(int total) {

        this.total = total;
    }

    public int getEpisodeDuration() {

        return this.episodeDuration;
    }

    public void setEpisodeDuration(int episodeDuration) {

        this.episodeDuration = episodeDuration;
    }

    public @NotNull DiscordUser getAddedBy() {

        return this.addedBy;
    }

    public void setAddedBy(@NotNull DiscordUser addedBy) {

        this.addedBy = addedBy;
    }

    public @Nullable Long getAnilistId() {

        return this.anilistId;
    }

    public void setAnilistId(@Nullable Long anilistId) {

        this.anilistId = anilistId;
    }

    public @Nullable Long getAnnouncementId() {

        return this.announcementId;
    }

    public void setAnnouncementId(@Nullable Long announcementId) {

        this.announcementId = announcementId;
    }

    public @NotNull Set<Episode> getEpisodes() {

        return this.episodes;
    }

    public void setEpisodes(@NotNull Set<Episode> episodes) {

        this.episodes = episodes;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Anime anime) return EntityUtils.equals(this, anime);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

    @Override
    public int compareTo(@NotNull Anime o) {

        return EntityUtils.compare(
                this,
                o,
                Comparator.comparing(Anime::getList),
                Comparator.comparing(Anime::getTitle)
        );
    }

    @Override
    public @NotNull String getScopedName() {

        if (this.isNew()) throw new IllegalStateException("Cannot use a non persisted entity as scoped entity.");
        return String.valueOf(this.getId());
    }

}
