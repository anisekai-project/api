package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.BaseEntity;
import fr.anisekai.server.domain.converters.UserFlagConverter;
import fr.anisekai.server.domain.enums.UserFlag;
import fr.anisekai.server.domain.events.user.UserEmoteUpdatedEvent;
import fr.anisekai.server.domain.events.user.UserUsernameUpdatedEvent;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;

@Entity
@Table(name = "user")
public class DiscordUser extends BaseEntity<Long> {

    @Id
    private Long id;

    @Column(nullable = false)
    @TriggerEvent(UserUsernameUpdatedEvent.class)
    private String username;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column
    @TriggerEvent(UserEmoteUpdatedEvent.class)
    private String emote;

    @Column(nullable = false)
    @Convert(converter = UserFlagConverter.class)
    private EnumSet<UserFlag> flags = EnumSet.noneOf(UserFlag.class);

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

    public EnumSet<UserFlag> getFlags() {

        return this.flags;
    }

    public void setFlags(EnumSet<UserFlag> flags) {

        this.flags = flags;
    }

    @Deprecated
    public boolean isActive() {

        return this.flags.contains(UserFlag.ACTIVE);
    }

    @Deprecated
    public void setActive(boolean active) {

        if (active) {
            this.flags.add(UserFlag.ACTIVE);
        } else {
            this.flags.remove(UserFlag.ACTIVE);
        }
    }

    @Deprecated
    public boolean isAdministrator() {

        return this.flags.contains(UserFlag.ADMINISTRATOR);
    }

    @Deprecated
    public void setAdministrator(boolean administrator) {

        if (administrator) {
            this.flags.add(UserFlag.ADMINISTRATOR);
        } else {
            this.flags.remove(UserFlag.ADMINISTRATOR);
        }
    }

    @Deprecated
    public boolean isGuest() {

        return !this.flags.contains(UserFlag.REGULAR);
    }

    @Deprecated
    public void setGuest(boolean guest) {

        if (!guest) {
            this.flags.add(UserFlag.REGULAR);
        } else {
            this.flags.remove(UserFlag.REGULAR);
        }
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
