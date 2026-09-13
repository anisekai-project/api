package fr.anisekai;

import fr.anisekai.discord.InteractionService;
import fr.anisekai.server.services.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ApplicationBootActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationBootActions.class);

    private final InteractionService discord;
    private final TaskService        taskService;

    public ApplicationBootActions(InteractionService discord, TaskService taskService) {

        this.discord     = discord;
        this.taskService = taskService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onBoot() {

        int recovered = this.taskService.recoverExecutingTasks();
        if (recovered > 0) LOGGER.info("Recovered {} interrupted local task(s)", recovered);
        this.discord.login();
    }

}
