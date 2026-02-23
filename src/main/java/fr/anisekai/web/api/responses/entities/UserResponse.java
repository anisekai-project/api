package fr.anisekai.web.api.responses.entities;

import fr.anisekai.server.domain.entities.DiscordUser;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Schema(description = "Structure of a user response.")
public record UserResponse(
        @Schema(
                description = "The user snowflake identifier.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "149279150648066048"
        ) @NotNull String id,

        @Schema(
                description = "The user discord username.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "akionakao"
        ) @NotNull String username,

        @Schema(
                description = "The user discord display name.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true,
                example = "AkioNakao"
        ) @Nullable String nickname,

        @Schema(
                description = "The user avatar url.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "https://cdn.discordapp.com/avatars/149279150648066048/617305f963d8485c4b6607265c9af7f7.png"
        ) @NotNull String avatar,

        @Schema(
                description = "The user vote emote.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "https://cdn.discordapp.com/avatars/149279150648066048/617305f963d8485c4b6607265c9af7f7.png"
        ) @Nullable String emote,

        @Schema(
                description = "This user's active state. An inactive user cannot take part in some event / polls.",
                requiredMode = Schema.RequiredMode.REQUIRED
        ) boolean active,

        @Schema(
                description = "This user's admin state.",
                requiredMode = Schema.RequiredMode.REQUIRED
        ) boolean administrator,

        @Schema(
                description = "This user's guest state. A guest user has a limited access to the website.",
                requiredMode = Schema.RequiredMode.REQUIRED
        ) boolean guest
) {

    public static UserResponse of(DiscordUser user) {

        return new UserResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getEmote(),
                user.isActive(),
                user.isAdministrator(),
                user.isGuest()
        );
    }

}
