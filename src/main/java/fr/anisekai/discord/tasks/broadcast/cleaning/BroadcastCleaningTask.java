package fr.anisekai.discord.tasks.broadcast.cleaning;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.sentry.ITimedAction;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.server.tasking.TaskExecutor;

public class BroadcastCleaningTask implements TaskExecutor {

    private final BroadcastService service;
    private final JDAStore         store;

    public BroadcastCleaningTask(BroadcastService service, JDAStore store) {

        this.service = service;
        this.store   = store;
    }

    @Override
    public void execute(ITimedAction timer, AnisekaiJson params) throws Exception {
        // TODO
        throw new IllegalAccessException("Task not implemented yet");
    }

}
