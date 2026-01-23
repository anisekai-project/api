package fr.anisekai.discord.interactions.broadcast;

import fr.alexpado.interactions.annotations.*;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.completions.AnimeCompletion;
import fr.anisekai.discord.completions.FrequencyCompletion;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.enums.BroadcastFrequency;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.utils.DateTimeUtils;
import fr.anisekai.utils.DiscordUtils;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@DiscordBean
@RequireAdmin
public class BroadcastSchedulingSlashInteraction {

    private final BroadcastService service;
    private final AnimeService     animeService;

    public BroadcastSchedulingSlashInteraction(AnimeService animeService, BroadcastService service) {

        this.animeService = animeService;
        this.service      = service;
    }

    @Slash(
            name = "broadcast/schedule",
            description = "\uD83D\uDD12 — Planifie une séance ou plusieurs séances de visionnage.",
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel la ou les séances seront planifiées.",
                            type = OptionType.INTEGER,
                            required = true,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "time",
                            description = "Heure à laquelle sera planifié la séance. (Format: HH:MM)",
                            type = OptionType.STRING,
                            required = true
                    ),
                    @Option(
                            name = "amount",
                            description = "Nombre d'épisode à planifier pour chaque séance.",
                            type = OptionType.INTEGER,
                            minInt = 1
                    ),
                    @Option(
                            name = "frequency",
                            description = "Fréquence à laquelle sera planifié chaque séance.",
                            type = OptionType.STRING,
                            completion = @Completion(named = FrequencyCompletion.NAME)
                    ),
                    @Option(
                            name = "starting",
                            description = "Date à partir de laquelle les séances seront planifiées. (Format: JJ/MM/AAAA)",
                            type = OptionType.STRING
                    ),
            }
    )
    @Deferrable
    public InteractionResponse executeSchedule(@Param("anime") long animeId, @Param("frequency") String frequencyName, @Param("time") String timeParam, @Param("amount") Long amount, @Param("starting") String startingParam) {

        Anime anime = this.animeService.requireById(animeId);

        BroadcastFrequency frequency = BroadcastFrequency.from(frequencyName);
        Instant            starting  = DateTimeUtils.of(timeParam, startingParam);

        if (DateTimeUtils.isBeforeOrEquals(starting, DateTimeUtils.now())) {
            return DiscordResponse.error("Impossible de plannifier des séances dans le passé.");
        }

        List<Broadcast> scheduled = this.service.schedule(
                anime,
                starting,
                frequency,
                Optional.of(amount).map(Long::intValue).orElse(1)
        );

        return DiscordResponse.info(switch (scheduled.size()) {
            case 0 -> "Aucune séance n'a été plannifiée.";
            case 1 -> "Une séance a été plannifiée pour l'anime **%s**.".formatted(DiscordUtils.link(anime));
            default -> "**%s** séances ont été plannifiées pour l'anime **%s**.".formatted(
                    scheduled.size(),
                    DiscordUtils.link(anime)
            );
        });
    }

    @Slash(
            name = "broadcast/delay",
            description = "\uD83D\uDD12 — Permet de reporter une ou plusieurs séances planifiées.",
            options = {
                    @Option(
                            name = "delay",
                            description = "La durée de report des séances. (Format: 0j00h00m)",
                            type = OptionType.STRING,
                            required = true
                    ),
                    @Option(
                            name = "range",
                            description = "Durée de l'intervale d'action du report de séances. (Par défaut: 6h) (Format: 0j00h00m)",
                            type = OptionType.STRING
                    ),
                    @Option(
                            name = "starting",
                            description = "Heure à partir de laquelle l'intervale commencera. (Par Défaut: Heure actuelle) (Format: HH:MM)",
                            type = OptionType.STRING
                    )
            }
    )
    @Deferrable
    public InteractionResponse executeDelay(@Param("delay") String delayParam, @Param("range") String rangeParam, @Param("starting") String startingParam) {

        Instant starting = DateTimeUtils.of(startingParam, null);
        Duration range = Optional.ofNullable(rangeParam)
                                 .map(DateTimeUtils::toDuration)
                                 .orElse(Duration.ofHours(6));
        Duration delay = DateTimeUtils.toDuration(delayParam);

        List<Broadcast> delayed = this.service.delay(starting, range, delay);

        return DiscordResponse.info(
                "%s évènement(s) mis à jour.",
                delayed.size()
        );
    }

}
