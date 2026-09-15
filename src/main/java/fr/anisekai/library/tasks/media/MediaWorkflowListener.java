package fr.anisekai.library.tasks.media;

import fr.anisekai.library.tasks.media.conversion.MediaConversionCompletedEvent;
import fr.anisekai.library.tasks.media.store.MediaStoreTaskFactory;
import fr.anisekai.library.tasks.media.store.MediaStoreTaskInput;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.services.TaskService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MediaWorkflowListener {

    private final TaskService taskService;

    public MediaWorkflowListener(TaskService taskService) {

        this.taskService = taskService;
    }

    @EventListener
    public void onConversionCompleted(MediaConversionCompletedEvent event) {

        this.taskService.queueOne(
                MediaStoreTaskFactory.class,
                new MediaStoreTaskInput(event.episodeId(), true),
                Task.PRIORITY_AUTOMATIC_HIGH
        );
    }

}
