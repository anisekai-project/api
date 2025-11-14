package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.BaseEntity;
import fr.anisekai.server.domain.events.user.UserActiveUpdatedEvent;
import fr.anisekai.server.domain.events.user.UserEmoteUpdatedEvent;
import fr.anisekai.server.domain.events.user.UserUsernameUpdatedEvent;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Entity(name = "user")
public class DiscordUser extends BaseEntity<Long> {

    @Id
    private Long id;

    @Column(nullable = false)
    @TriggerEvent(UserUsernameUpdatedEvent.class)
    private String username;

    @Column(nullable = false)
    private String nickname;

    @Column
    private String avatarUrl;

    @Column
    @TriggerEvent(UserEmoteUpdatedEvent.class)
    private String emote;

    @Column(nullable = false)
    @TriggerEvent(UserActiveUpdatedEvent.class)
    private boolean active = false;

    @Column(nullable = false)
    private boolean administrator = false;

    @Column(nullable = false)
    private boolean guest = true;

    @Column
    @Deprecated
    private String apiKey;

    @Override
    public Long getId() {

        return this.id;
    }

    @Override
    public void setId(Long id) {

        this.id = id;
    }

    public @NotNull String getUsername() {

        return this.username;
    }

    public void setUsername(@NotNull String username) {

        this.username = username;
    }

    public @Nullable String getNickname() {

        return this.nickname;
    }

    public void setNickname(String nickname) {

        this.nickname = nickname;
    }

    public @NotNull String getAvatarUrl() {

        return this.avatarUrl;
    }

    public void setAvatarUrl(@NotNull String avatarUrl) {

        this.avatarUrl = avatarUrl;
    }

    public @Nullable String getEmote() {

        return this.emote;
    }

    public void setEmote(String emote) {

        this.emote = emote;
    }

    public boolean isActive() {

        return this.active;
    }

    public void setActive(boolean active) {

        this.active = active;
    }

    public boolean isAdministrator() {

        return this.administrator;
    }

    public void setAdministrator(boolean administrator) {

        this.administrator = administrator;
    }

    public boolean isGuest() {

        return this.guest;
    }

    public void setGuest(boolean guest) {

        this.guest = guest;
    }

    @Deprecated
    public @Nullable String getApiKey() {

        return this.apiKey;
    }

    @Deprecated
    public void setApiKey(String apiKey) {

        this.apiKey = apiKey;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof DiscordUser user) return EntityUtils.equals(this, user);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
