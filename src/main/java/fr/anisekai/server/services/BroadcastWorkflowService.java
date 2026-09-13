package fr.anisekai.server.services;

import fr.anisekai.discord.tasks.broadcast.cancel.BroadcastCancelTaskFactory;
import fr.anisekai.discord.tasks.broadcast.cancel.BroadcastCancelTaskInput;
import fr.anisekai.discord.tasks.broadcast.schedule.BroadcastScheduleTaskFactory;
import fr.anisekai.discord.tasks.broadcast.schedule.BroadcastScheduleTaskInput;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.domain.enums.BroadcastStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BroadcastWorkflowService {

    private final BroadcastService             broadcastService;
    private final TaskService                  taskService;
    private final BroadcastCancelTaskFactory   cancelFactory;
    private final BroadcastScheduleTaskFactory scheduleFactory;

    public BroadcastWorkflowService(BroadcastService broadcastService, TaskService taskService, BroadcastCancelTaskFactory cancelFactory, BroadcastScheduleTaskFactory scheduleFactory) {

        this.broadcastService = broadcastService;
        this.taskService      = taskService;
        this.cancelFactory    = cancelFactory;
        this.scheduleFactory  = scheduleFactory;
    }

    public void queueScheduling(Broadcast broadcast) {

        this.taskService.queueOne(
                BroadcastScheduleTaskFactory.class,
                BroadcastScheduleTaskFactory.createInput(broadcast),
                Task.PRIORITY_DEFAULT
        );
    }

    private void queueCancellation(Broadcast broadcast) {

        this.taskService.queueOne(
                BroadcastCancelTaskFactory.class,
                new BroadcastCancelTaskInput(broadcast.getId()),
                Task.PRIORITY_DEFAULT
        );
    }

    public int refresh() {

        List<Broadcast> broadcasts = this.broadcastService.getRepository().findAllByStatus(BroadcastStatus.SCHEDULED);
        broadcasts.forEach(this::queueScheduling);
        return broadcasts.size();
    }

    @Transactional
    public int cancel() {

        List<Broadcast> broadcasts = this.broadcastService.getRepository().findAllByStatus(BroadcastStatus.ACTIVE);
        for (Broadcast broadcast : broadcasts) {
            BroadcastCancelTaskInput cancelInput = new BroadcastCancelTaskInput(broadcast.getId());
            if (this.taskService.hasScheduled(this.cancelFactory.getTaskName(cancelInput))) continue;

            BroadcastScheduleTaskInput scheduleInput = BroadcastScheduleTaskFactory.createInput(broadcast);
            this.taskService.cancel(this.scheduleFactory.getTaskName(scheduleInput));

            if (broadcast.getStatus().isDiscordCancelable()) {
                this.queueCancellation(broadcast);
            } else {
                broadcast.setStatus(BroadcastStatus.CANCELED);
                this.broadcastService.getRepository().save(broadcast);
            }
        }
        return broadcasts.size();
    }

    public Broadcast cancel(Broadcast broadcast) {

        BroadcastCancelTaskInput input = new BroadcastCancelTaskInput(broadcast.getId());
        String taskName = this.cancelFactory.getTaskName(input);
        if (this.taskService.hasScheduled(taskName)) this.taskService.cancel(taskName);

        broadcast.setStatus(BroadcastStatus.CANCELED);
        return this.broadcastService.getRepository().save(broadcast);
    }

}
