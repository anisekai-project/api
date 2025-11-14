package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.BaseEntity;
import fr.anisekai.server.domain.events.voter.VoterAmountUpdatedEvent;
import fr.anisekai.server.domain.events.voter.VoterVotesUpdatedEvent;
import fr.anisekai.server.domain.keys.VoterKey;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Entity
@IdClass(VoterKey.class)
public class Voter extends BaseEntity<VoterKey> {

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Selection selection;

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private DiscordUser user;

    @Column(nullable = false)
    @TriggerEvent(VoterAmountUpdatedEvent.class)
    private short amount;

    @OneToMany(fetch = FetchType.EAGER)
    @TriggerEvent(VoterVotesUpdatedEvent.class)
    private Set<Anime> votes;

    @Override
    public VoterKey getId() {

        return VoterKey.create(this.getSelection(), this.getUser());
    }

    public @NotNull Selection getSelection() {

        return this.selection;
    }

    public void setSelection(@NotNull Selection selection) {

        this.selection = selection;
    }

    public @NotNull DiscordUser getUser() {

        return this.user;
    }

    public void setUser(@NotNull DiscordUser user) {

        this.user = user;
    }

    public short getAmount() {

        return this.amount;
    }

    public void setAmount(short amount) {

        this.amount = amount;
    }

    public @NotNull Set<Anime> getVotes() {

        return this.votes;
    }

    public void setVotes(@NotNull Set<Anime> votes) {

        this.votes = votes;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Voter voter) return EntityUtils.equals(this, voter);
        return false;
    }

}
