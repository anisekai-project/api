package fr.anisekai.core.persistence;

import fr.anisekai.core.persistence.interfaces.CloseableContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AnisekaiServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnisekaiServiceRegistry.class);

    private final ListableBeanFactory           factory;
    private final Set<AnisekaiService<?, ?, ?>> services;

    public AnisekaiServiceRegistry(ListableBeanFactory factory) {

        this.factory  = factory;
        this.services = new HashSet<>();
    }

    @PostConstruct
    protected void discover() {

        LOGGER.info("Discovering services...");

        String[] names = this.factory.getBeanNamesForType(AnisekaiService.class);

        for (String name : names) {
            AnisekaiService<?, ?, ?> service = this.factory.getBean(name, AnisekaiService.class);
            LOGGER.info(" -> Discovered generic service {}", service.getClass().getSimpleName());
            this.services.add(service);
        }

        LOGGER.info("Discovered {} services.", this.services.size());
    }

    /**
     * Creates a single, composite context that activates event support for ALL registered services for the scope of a
     * try-with-resources block.
     */
    public CloseableContext openEventContexts() {

        LOGGER.info("Starting global event context...");

        List<CloseableContext> contexts = this.services
                .stream()
                .map(AnisekaiService::withEventsSupport)
                .toList();

        return () -> {
            LOGGER.info("Stopping global event context...");
            contexts.forEach(CloseableContext::close);
        };
    }

}
