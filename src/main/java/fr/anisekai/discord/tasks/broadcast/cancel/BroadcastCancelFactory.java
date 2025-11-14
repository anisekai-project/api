package fr.anisekai.discord.tasks.broadcast.cancel;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.discord.tasks.broadcast.BroadcastFactory;
import fr.anisekai.discord.tasks.broadcast.BroadcastTask;
import fr.anisekai.library.Library;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.enums.TaskPipeline;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.tasking.TaskBuilder;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class BroadcastCancelFactory extends BroadcastFactory<BroadcastCancelTask> {

    private static final String NAME = "cancel";

    public BroadcastCancelFactory(Library library, TaskService service, JDAStore store, BroadcastService broadcastService) {

        super(library, service, store, broadcastService);
    }

    @Override
    public @NotNull String getName() {

        return String.format("%s:%s", this.getPrefix(), NAME);
    }

    @Override
    public @NotNull BroadcastCancelTask create() {

        return new BroadcastCancelTask(this.getLibrary(), this.getStore(), this.getBroadcastService());
    }

    public Task queue(Broadcast broadcast) {

        return this.queue(broadcast, Task.PRIORITY_AUTOMATIC_LOW);
    }

    public Task queue(Broadcast broadcast, byte priority) {

        String       name      = String.format("%s:%s", this.getName(), broadcast.getId());
        AnisekaiJson arguments = new AnisekaiJson();
        arguments.put(BroadcastTask.OPT_BROADCAST, broadcast.getId());

        return this.getService().queue(
                TaskBuilder.of(this)
                           .name(name)
                           .args(arguments)
                           .priority(priority)
        );
    }

    @PostConstruct
    public void postConstruct() {

        this.getService().registerFactory(TaskPipeline.SOFT, this);
    }

}
