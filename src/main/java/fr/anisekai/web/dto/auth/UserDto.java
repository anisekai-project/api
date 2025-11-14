package fr.anisekai.web.dto.auth;

import fr.anisekai.server.domain.entities.DiscordUser;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "User", description = "Represent a user")
public record UserDto(
        @Schema(description = "This user's discord identifier")
        String id,
        @Schema(description = "This user's username")
        String username,
        @Schema(description = "This user's nickname")
        String nickname,
        @Schema(description = "This user's avatar url")
        String avatarUrl,
        @Schema(description = "This user's vote emote")
        String emote,
        @Schema(description = "This user's active state")
        boolean active,
        @Schema(description = "This user's admin state")
        boolean administrator,
        @Schema(description = "This user's guest state")
        boolean guest
) {

    public static UserDto of(DiscordUser user) {

        return new UserDto(
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
