package fr.anisekai;

import fr.anisekai.discord.InteractionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAutoStarts {

    private final InteractionService discord;

    public ApplicationAutoStarts(InteractionService discord) {

        this.discord = discord;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onBoot() {

        this.discord.login();
    }

}
