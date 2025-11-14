package fr.anisekai.server.exceptions.task;

import fr.anisekai.core.annotations.FatalTask;
import fr.anisekai.server.domain.entities.Task;
import fr.anisekai.server.tasking.TaskFactory;

@FatalTask
public class FactoryNotFoundException extends RuntimeException {

    public FactoryNotFoundException(Task task) {

        super(String.format(
                "The factory of name '%s' does not exist or has not been registered.",
                task.getFactoryName()
        ));
    }

    public FactoryNotFoundException(Class<? extends TaskFactory<?>> factoryClass) {

        super(String.format(
                "The factory of class '%s' does not exist or has not been registered.",
                factoryClass.getSimpleName()
        ));
    }

}
