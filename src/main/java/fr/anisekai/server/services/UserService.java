package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.repositories.UserRepository;
import fr.anisekai.web.packets.results.DiscordIdentity;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService extends AnisekaiService<DiscordUser, Long, UserRepository> {

    public UserService(UserRepository repository, EntityEventProcessor eventProcessor) {

        super(repository, eventProcessor);
    }

    public DiscordUser of(User user) {

        return this.upsert(
                repository -> repository.findById(user.getIdLong()),
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

        return this.upsert(
                repository -> repository.findById(identity.getId()),
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

    @Deprecated
    public Optional<DiscordUser> getByApiKey(String apiKey) {

        return this.getRepository().findByApiKey(apiKey);
    }

    public boolean canUseEmote(DiscordUser requestingUser, String emote) {

        return this.getRepository()
                   .findAll()
                   .stream()
                   .filter(user -> !Objects.isNull(user.getEmote()))
                   .filter(user -> !Objects.equals(user.getId(), requestingUser.getId()))
                   .noneMatch(user -> user.getEmote().equals(emote));
    }

    public DiscordUser useEmote(DiscordUser requestingUser, String emote) {

        requestingUser.setEmote(emote);
        return this.getRepository().save(requestingUser);
    }

    public List<DiscordUser> getActiveUsers() {

        return this.getRepository().findAllByActiveIsTrue();
    }

    public Optional<DiscordUser> findFromIdentity(DiscordIdentity identity) {

        return this.getRepository().findById(identity.getId());
    }

}
