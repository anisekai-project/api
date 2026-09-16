package fr.anisekai.server.exceptions.task;

public class WorkerDesyncException extends RuntimeException {

    public WorkerDesyncException(String message) {

        super(message);
    }
}
