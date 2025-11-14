package fr.anisekai.discord.interactions.administation;

import fr.alexpado.jda.interactions.annotations.Interact;
import fr.alexpado.jda.interactions.annotations.Option;
import fr.alexpado.jda.interactions.annotations.Param;
import fr.alexpado.jda.interactions.responses.SlashResponse;
import fr.anisekai.core.internal.plannifier.data.CalibrationResult;
import fr.anisekai.discord.annotations.InteractionBean;
import fr.anisekai.discord.exceptions.RequireAdministratorException;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.enums.BroadcastFrequency;
import fr.anisekai.server.services.AnimeService;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.utils.DateTimeUtils;
import fr.anisekai.utils.DiscordUtils;
import fr.anisekai.utils.StringUtils;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Component
@InteractionBean
public class BroadcastInteractions {

    private final AnimeService     animeService;
    private final BroadcastService service;

    public BroadcastInteractions(AnimeService animeService, BroadcastService service) {

        this.animeService = animeService;
        this.service      = service;
    }

    private static void requireAdministrator(DiscordUser user) {

        if (!user.isAdministrator()) {
            throw new RequireAdministratorException();
        }
    }

    // <editor-fold desc="@ broadcast/schedule ─ Schedule one or more watch party. [anime: integer, time: string, amount: string, frequency: ?string, starting: ?string]">
    @Interact(
            name = "broadcast/schedule",
            description = "\uD83D\uDD12 — Planifie une séance ou plusieurs séances de visionnage.",
            defer = true,
            options = {
                    @Option(
                            name = "anime",
                            description = "Anime pour lequel la ou les séances seront planifiées.",
                            type = OptionType.INTEGER,
                            autoComplete = true,
                            required = true
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
                            required = true
                    ),
                    @Option(
                            name = "frequency",
                            description = "Fréquence à laquelle sera planifié chaque séance.",
                            type = OptionType.STRING,
                            autoComplete = true
                    ),
                    @Option(
                            name = "starting",
                            description = "Date à partir de laquelle les séances seront planifiées. (Format: JJ/MM/AAAA)",
                            type = OptionType.STRING
                    )
            }
    )
    public SlashResponse broadcastSchedule(DiscordUser user, @Param("anime") long animeId, @Param("frequency") String frequencyName, @Param("time") String timeParam, @Param("amount") long amount, @Param("starting") String startingParam) {

        requireAdministrator(user);
        Anime anime = this.animeService.requireById(animeId);

        BroadcastFrequency frequency = BroadcastFrequency.from(frequencyName);
        ZonedDateTime      starting  = DateTimeUtils.of(timeParam, startingParam);

        if (DateTimeUtils.isBeforeOrEquals(starting, DateTimeUtils.now())) {
            return DiscordResponse.error("Impossible de plannifier des séances dans le passé.");
        }

        List<Broadcast> scheduled = this.service.schedule(anime, starting, frequency, (int) amount);

        return DiscordResponse.info(switch (scheduled.size()) {
            case 0 -> "Aucune séance n'a été plannifiée.";
            case 1 -> "Une séance a été plannifiée pour l'anime **%s**.".formatted(DiscordUtils.link(anime));
            default -> "**%s** séances ont été plannifiées pour l'anime **%s**.".formatted(
                    scheduled.size(),
                    DiscordUtils.link(anime)
            );
        });
    }
    // </editor-fold>

    // <editor-fold desc="@ broadcast/calibrate ─ Run a manual calibration of all watch party.">
    @Interact(
            name = "broadcast/calibrate",
            description = "\uD83D\uDD12 — Permet de lancer une calibration manuelle des séances.",
            defer = true
    )
    public SlashResponse broadcastCalibration(DiscordUser user) {

        requireAdministrator(user);
        CalibrationResult calibrate = this.service.calibrate();
        return DiscordResponse.success(
                "Le planning a été calibré.\n%s évènement(s) mis à jour.\n%s évènement(s) supprimé(s).",
                calibrate.updateCount(),
                calibrate.deleteCount()
        );
    }
    // </editor-fold>

    // <editor-fold desc="@ broadcast/delay ─ Delay one or multiple scheduled watch party. [delay: string, range: ?string, starting: ?string">
    @Interact(
            name = "broadcast/delay",
            description = "\uD83D\uDD12 — Permet de reporter une ou plusieurs séances planifiées.",
            defer = true,
            options = {
                    @Option(
                            name = "delay",
                            description = "La durée de report des séances. (Format: 0j00h00m)",
                            type = OptionType.STRING,
                            required = true
                    ),
                    @Option(
                            name = "range",
                            description = "Durée de l'interval d'action du report de séances. (Par défaut: 6h) (Format: 0j00h00m)",
                            type = OptionType.STRING
                    ),
                    @Option(
                            name = "starting",
                            description = "Heure à partir de laquelle l'interval commencera. (Par Défaut: Heure actuelle) (Format: HH:MM)",
                            type = OptionType.STRING
                    )
            }
    )
    public SlashResponse broadcastDelay(DiscordUser user, @Param("delay") String delayParam, @Param("range") String rangeParam, @Param("starting") String startingParam) {


        ZonedDateTime starting = DateTimeUtils.of(startingParam, null);
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
    // </editor-fold>

    // <editor-fold desc="@ broadcast/cancel ─ Cancel a watch party that has already started.">
    @Interact(
            name = "broadcast/cancel",
            description = "\uD83D\uDD12 — Annule une séance de visionnage déjà en cours."
    )
    public SlashResponse broadcastCancel() {

        int amount = this.service.cancel();

        return DiscordResponse.info(StringUtils.count(
                amount,
                "Aucun évènement annulé.",
                "Un évènement a été annulé.",
                "%s évènements ont été annulés"
        ));
    }
    // </editor-fold>

    // <editor-fold desc="@ broadcast/refresh ─ Refresh all scheduled event with the database status.">
    @Interact(
            name = "broadcast/refresh",
            description = "\uD83D\uDD12 — Actualise les évènements plannifiés du serveur."
    )
    public SlashResponse broadcastRefresh(DiscordUser user) {

        int amount = this.service.refresh();

        return DiscordResponse.info(StringUtils.count(
                amount,
                "Aucun évènement actualisé.",
                "Un évènement a été actualisé.",
                "%s évènements ont été actualisés"
        ));
    }
    // </editor-fold>

}
