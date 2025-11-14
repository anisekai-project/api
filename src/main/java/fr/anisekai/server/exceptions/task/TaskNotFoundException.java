package fr.anisekai.server.exceptions.task;

import fr.anisekai.core.annotations.FatalTask;
import fr.anisekai.server.exceptions.EntityNotFoundException;

@FatalTask
public class TaskNotFoundException extends EntityNotFoundException {

}
