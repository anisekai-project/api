package fr.anisekai.server.exceptions.task;

import fr.anisekai.core.annotations.FatalTask;

@FatalTask
public class FatalTaskException extends RuntimeException {

    public FatalTaskException(String message) {

        super(message);
    }

}
