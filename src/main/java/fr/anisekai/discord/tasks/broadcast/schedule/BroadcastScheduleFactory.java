package fr.anisekai.discord.tasks.broadcast.schedule;

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
public class BroadcastScheduleFactory extends BroadcastFactory<BroadcastScheduleTask> {

    private static final String NAME = "schedule";

    public BroadcastScheduleFactory(Library library, TaskService service, JDAStore store, BroadcastService broadcastService) {

        super(library, service, store, broadcastService);
    }

    @Override
    public @NotNull String getName() {

        return String.format("%s:%s", this.getPrefix(), NAME);
    }

    @Override
    public @NotNull BroadcastScheduleTask create() {

        return new BroadcastScheduleTask(this.getLibrary(), this.getStore(), this.getBroadcastService());
    }

    public Task queue(Broadcast broadcast) {

        return this.queue(broadcast, Task.PRIORITY_AUTOMATIC_LOW);
    }

    public Task queue(Broadcast broadcast, byte priority) {

        String       name      = this.asTaskName(broadcast);
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
    private void postConstruct() {

        this.getService().registerFactory(TaskPipeline.SOFT, this);
    }

}
