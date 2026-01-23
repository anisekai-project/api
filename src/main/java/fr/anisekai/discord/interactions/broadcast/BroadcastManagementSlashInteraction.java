package fr.anisekai.discord.interactions.broadcast;

import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.core.internal.plannifier.data.CalibrationResult;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.library.Library;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.utils.StringUtils;

@DiscordBean
@RequireAdmin
public class BroadcastManagementSlashInteraction {

    private final BroadcastService service;

    public BroadcastManagementSlashInteraction(BroadcastService service, Library library) {

        this.service = service;
    }

    @Slash(
            name = "broadcast/calibrate",
            description = "\uD83D\uDD12 — Permet de lancer une calibration manuelle des séances."
    )
    @Deprecated
    public InteractionResponse executeCalibrate() {

        CalibrationResult calibrate = this.service.calibrate();
        return DiscordResponse.success(
                "Le planning a été calibré.\n%s évènement(s) mis à jour.\n%s évènement(s) supprimé(s).",
                calibrate.updateCount(),
                calibrate.deleteCount()
        );
    }

    @Slash(
            name = "broadcast/cancel",
            description = "\uD83D\uDD12 — Annule une séance de visionnage déjà en cours."
    )
    public InteractionResponse executeCancel() {

        int amount = this.service.cancel();

        return DiscordResponse.info(StringUtils.count(
                amount,
                "Aucun évènement annulé.",
                "Un évènement a été annulé.",
                "%s évènements ont été annulés"
        ));
    }

    @Slash(
            name = "broadcast/refresh",
            description = "\uD83D\uDD12 — Annule une séance de visionnage déjà en cours."
    )
    public InteractionResponse executeRefresh() {

        int amount = this.service.refresh();

        return DiscordResponse.info(StringUtils.count(
                amount,
                "Aucun évènement actualisé.",
                "Un évènement a été actualisé.",
                "%s évènements ont été actualisés"
        ));
    }


}
