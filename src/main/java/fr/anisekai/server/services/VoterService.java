package fr.anisekai.server.services;

import fr.anisekai.core.persistence.AnisekaiService;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.Selection;
import fr.anisekai.server.domain.entities.Voter;
import fr.anisekai.server.domain.keys.VoterKey;
import fr.anisekai.server.exceptions.selection.SelectionAnimeNotFoundException;
import fr.anisekai.server.exceptions.voter.VoterMaxReachedException;
import fr.anisekai.server.repositories.VoterRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoterService extends AnisekaiService<Voter, VoterKey, VoterRepository> {

    private final UserService userService;

    public VoterService(VoterRepository repository, EntityEventProcessor eventProcessor, UserService userService) {

        super(repository, eventProcessor);
        this.userService = userService;
    }

    public void castVote(Selection selection, DiscordUser user, Anime anime) {

        if (!selection.getAnimes().contains(anime)) {
            throw new SelectionAnimeNotFoundException();
        }

        Voter voter = this.require(repository -> repository.findBySelectionAndUser(selection, user));

        if (voter.getVotes().contains(anime)) {
            voter.getVotes().remove(anime);
            this.getRepository().save(voter);
            return;
        }

        if (voter.getAmount() == voter.getVotes().size()) {
            throw new VoterMaxReachedException();
        }

        voter.getVotes().add(anime);
        this.getRepository().save(voter);
    }

    public List<Voter> getVoters(Selection selection) {

        return this.getRepository().findBySelection(selection);
    }

    public List<Voter> createVoters(Selection selection, long maxVote) {

        List<DiscordUser>       activeUsers = this.userService.getActiveUsers();
        Map<DiscordUser, Short> voteMap     = new HashMap<>();
        activeUsers.forEach(user -> voteMap.put(user, (short) 0));

        long voteLeft = maxVote;
        int  i        = 0;
        while (voteLeft > 0) {
            DiscordUser user = activeUsers.get(i);
            voteMap.put(user, (short) (voteMap.get(user) + 1));
            voteLeft -= 1;
            i = activeUsers.size() - 1 == i ? 0 : i + 1;
        }

        return activeUsers.stream()
                          .map(user -> this.createVoter(selection, user, voteMap.get(user)))
                          .toList();
    }

    public Voter createVoter(Selection selection, DiscordUser user, short amount) {

        Voter voter = new Voter();

        voter.setSelection(selection);
        voter.setAmount(amount);
        voter.setUser(user);

        return this.getRepository().save(voter);
    }

}
