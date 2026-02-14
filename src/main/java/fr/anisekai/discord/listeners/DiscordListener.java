package fr.anisekai.discord.listeners;

import fr.anisekai.core.internal.plannifier.data.CalibrationResult;
import fr.anisekai.core.persistence.EventContextRegistry;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.responses.embeds.CalibrationEmbed;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import fr.anisekai.server.services.BroadcastService;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventDeleteEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.update.ScheduledEventUpdateStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DiscordListener extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordListener.class);

    private final EventContextRegistry registry;
    private final JDAStore             store;
    private final BroadcastService     service;

    public DiscordListener(EventContextRegistry registry, JDAStore store, BroadcastService service) {

        this.registry = registry;
        this.store    = store;
        this.service  = service;
    }

    private void cancel(ScheduledEvent event) {

        this.registry.withEventContext(() -> {
            Optional<Broadcast> optionalBroadcast = this.service.find(event);

            if (optionalBroadcast.isPresent()) {
                Broadcast broadcast = optionalBroadcast.get();
                this.service.cancel(broadcast);

                CalibrationResult calibrate = this.service.calibrate();

                this.store.getAuditChannel().ifPresent(channel -> {
                    MessageCreateBuilder mcb   = new MessageCreateBuilder();
                    CalibrationEmbed     embed = new CalibrationEmbed();
                    embed.setCalibrationResult(broadcast, calibrate);
                    mcb.setEmbeds(embed.build());
                    channel.sendMessage(mcb.build()).queue();
                });
            }
        });
    }

    @Override
    public void onScheduledEventDelete(@NonNull ScheduledEventDeleteEvent event) {

        this.registry.withEventContext(() -> this.cancel(event.getScheduledEvent()));
    }

    @Override
    public void onScheduledEventUpdateStatus(@NonNull ScheduledEventUpdateStatusEvent event) {

        this.registry.withEventContext(() -> {
            Optional<Broadcast> optionalBroadcast = this.service.find(event.getScheduledEvent());
            if (optionalBroadcast.isEmpty()) {
                LOGGER.info("Ignoring event {}: This is not a broadcast.", event.getScheduledEvent().getId());
                return;
            }

            Broadcast broadcast = optionalBroadcast.get();
            LOGGER.info("Updating broadcast {} using event status {}", broadcast.getId(), event.getNewStatus());

            switch (event.getNewStatus()) {
                case ACTIVE -> this.service.mod(
                        broadcast.getId(),
                        entity -> entity.setStatus(BroadcastStatus.ACTIVE)
                );
                case COMPLETED -> this.service.mod(
                        broadcast.getId(),
                        entity -> entity.setStatus(BroadcastStatus.COMPLETED)
                );
                case CANCELED -> this.cancel(event.getScheduledEvent());
            }
        });
    }

}
