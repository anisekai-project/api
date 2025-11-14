package fr.anisekai.server.exceptions.task;

import fr.anisekai.core.annotations.FatalTask;
import org.json.JSONException;

@FatalTask
public class TaskArgumentException extends Exception {

    public TaskArgumentException(String message, JSONException ex) {

        super(message, ex);
    }

}
