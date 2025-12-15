package fr.anisekai.discord.resolvers;

import fr.alexpado.interactions.providers.interactions.slash.SlashRouteResolver;
import fr.alexpado.interactions.providers.interactions.slash.interfaces.CompletionProvider;
import fr.alexpado.interactions.structure.Endpoint;
import fr.anisekai.discord.annotations.CompletionBean;
import fr.anisekai.discord.annotations.DiscordBean;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SlashInteractionResolver extends SlashRouteResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlashInteractionResolver.class);

    private final ListableBeanFactory factory;

    public SlashInteractionResolver(ListableBeanFactory factory) {

        this.factory = factory;
    }

    @PostConstruct
    private void init() {

        this.discoverInteractions();
        this.discoverCompletions();
    }

    private void discoverInteractions() {

        LOGGER.info("Finding completion providers...");
        Map<String, Object> beanMap = this.factory.getBeansWithAnnotation(CompletionBean.class);

        for (String name : beanMap.keySet()) {
            Object bean = beanMap.get(name);

            if (bean instanceof CompletionProvider provider) {
                LOGGER.info(" -> Found {} completion provider", name);
                this.registerCompletionProvider(name, provider);
            }
        }

        LOGGER.info("Discovering slash interaction beans...");
        Map<String, Object> beans = this.factory.getBeansWithAnnotation(DiscordBean.class);

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            List<Endpoint<?>> endpoints = this.registerController(entry.getValue());

            if (endpoints.isEmpty()) {
                LOGGER.warn(
                        " -> Discovered slash interaction bean {} but no endpoint could be extracted.",
                        entry.getKey()
                );
                continue;
            }

            LOGGER.info(
                    " -> Discovered slash interaction bean {} with {} endpoint(s).",
                    entry.getKey(),
                    endpoints.size()
            );

            if (LOGGER.isTraceEnabled()) {
                for (Endpoint<?> endpoint : endpoints) {
                    LOGGER.trace(" | -> Endpoint {}", endpoint.route().getUri());
                }
            }
        }
    }

    private void discoverCompletions() {

        LOGGER.info("Discovering completion beans...");
        Map<String, Object> beans = this.factory.getBeansWithAnnotation(CompletionBean.class);

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (!(entry.getValue() instanceof CompletionProvider provider)) {
                throw new IllegalArgumentException(String.format(
                        "Bean '%s' annotated with @%s but does not implement %s.",
                        entry.getKey(),
                        CompletionBean.class.getSimpleName(),
                        CompletionProvider.class.getSimpleName()
                ));
            }

            this.registerCompletionProvider(entry.getKey(), provider);
            LOGGER.info(" -> Discovered completion provider {}.", entry.getKey());
        }
    }

}
