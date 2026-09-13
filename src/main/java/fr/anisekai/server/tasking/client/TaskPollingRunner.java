package fr.anisekai.server.tasking.client;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskPollingRunner {

    private final ClientOrchestrator orchestrator;
    private volatile boolean ready;

    public TaskPollingRunner(ClientOrchestrator orchestrator) {

        this.orchestrator = orchestrator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void enable() {

        this.ready = true;
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void tick() {

        if (this.ready) this.orchestrator.tick();
    }

}
