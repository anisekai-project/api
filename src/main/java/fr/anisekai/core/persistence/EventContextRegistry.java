package fr.anisekai.core.persistence;

import fr.anisekai.core.interfaces.ThrowableFunction;
import fr.anisekai.core.persistence.interfaces.CloseableContext;
import fr.anisekai.core.persistence.interfaces.EventContextProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EventContextRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventContextRegistry.class);

    private final ListableBeanFactory       factory;
    private final Set<EventContextProvider> providers;

    public EventContextRegistry(ListableBeanFactory factory) {

        this.factory   = factory;
        this.providers = new HashSet<>();
    }

    @PostConstruct
    protected void discover() {

        LOGGER.info("Discovering event context providers...");

        String[] names = this.factory.getBeanNamesForType(EventContextProvider.class);

        for (String name : names) {
            EventContextProvider provider = this.factory.getBean(name, EventContextProvider.class);
            LOGGER.info(" -> Discovered provider {}", provider.getClass().getSimpleName());
            this.providers.add(provider);
        }

        LOGGER.info("Discovered {} services.", this.providers.size());
    }

    /**
     * Open an event context and returns the {@link CloseableContext} resource that can close that event context.
     *
     * @return A {@link CloseableContext} resource to close when the event context can be dropped.
     */
    public CloseableContext startEventContexts() {

        LOGGER.debug("Starting global event context");

        List<CloseableContext> contexts = this.providers
                .stream()
                .map(EventContextProvider::startEventContext)
                .toList();

        return () -> {
            LOGGER.debug("Stopping global event context...");
            contexts.forEach(CloseableContext::close);
        };
    }

    /**
     * Run a {@link ThrowableFunction} within the event context of this {@link EventContextRegistry}. The event context
     * will be automatically closed at the end of the execution flow, whether it succeeded or failed.
     *
     * @param action
     *         The {@link ThrowableFunction} to run within the event context
     * @param <E>
     *         Type of the {@link Throwable} that can be thrown during the execution of the {@link ThrowableFunction}.
     *
     * @throws E
     *         if the {@link ThrowableFunction} execution could not be completed.
     */
    public <E extends Throwable> void withEventContext(ThrowableFunction<E> action) throws E {

        try (CloseableContext _ = this.startEventContexts()) {
            action.run();
        }
    }

}
