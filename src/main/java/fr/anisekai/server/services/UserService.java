package fr.anisekai.server.services;

import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.repositories.UserRepository;
import fr.anisekai.web.packets.results.DiscordIdentity;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {

        this.repository = repository;
    }

    public DiscordUser of(User user) {

        return this.repository.upsert(
                () -> this.repository.findById(user.getIdLong()),
                () -> {
                    DiscordUser discordUser = new DiscordUser();
                    discordUser.setId(user.getIdLong());
                    return discordUser;
                },
                discordUser -> {
                    discordUser.setUsername(user.getName());
                    discordUser.setAvatarUrl(user.getEffectiveAvatarUrl());
                    discordUser.setNickname(user.getGlobalName());
                }
        ).entity();
    }

    public DiscordUser ensureUserExists(DiscordIdentity identity) {

        return this.repository.upsert(
                () -> this.repository.findById(identity.getId()),
                () -> {
                    DiscordUser discordUser = new DiscordUser();
                    discordUser.setId(identity.getId());
                    return discordUser;
                },
                discordUser -> {
                    discordUser.setUsername(identity.getUsername());
                    discordUser.setAvatarUrl(identity.getAvatar());
                    discordUser.setNickname(identity.getUsername());
                }
        ).entity();

    }

    public boolean canUseEmote(DiscordUser requestingUser, String emote) {

        return this.repository.findAll()
                              .stream()
                              .filter(user -> !Objects.isNull(user.getEmote()))
                              .filter(user -> !Objects.equals(user.getId(), requestingUser.getId()))
                              .noneMatch(user -> user.getEmote().equals(emote));
    }

    public DiscordUser useEmote(DiscordUser requestingUser, String emote) {

        requestingUser.setEmote(emote);
        return this.repository.save(requestingUser);
    }

    public List<DiscordUser> getActiveUsers() {

        return this.repository.findAllByActiveIsTrue();
    }

    public Optional<DiscordUser> findFromIdentity(DiscordIdentity identity) {

        return this.repository.findById(identity.getId());
    }

}
