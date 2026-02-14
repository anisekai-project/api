package fr.anisekai.core.interfaces;

public interface ThrowableFunction<E extends Throwable> {

    void run() throws E;

}
