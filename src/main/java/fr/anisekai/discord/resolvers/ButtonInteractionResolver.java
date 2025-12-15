package fr.anisekai.discord.resolvers;

import fr.alexpado.interactions.providers.interactions.button.ButtonRouteResolver;
import fr.alexpado.interactions.structure.Endpoint;
import fr.anisekai.discord.annotations.DiscordBean;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ButtonInteractionResolver extends ButtonRouteResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ButtonInteractionResolver.class);

    private final ListableBeanFactory factory;

    public ButtonInteractionResolver(ListableBeanFactory factory) {

        this.factory = factory;
    }

    @PostConstruct
    private void init() {

        LOGGER.info("Discovering button interaction beans...");
        Map<String, Object> beans = this.factory.getBeansWithAnnotation(DiscordBean.class);

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            List<Endpoint<ButtonInteraction>> endpoints = this.registerController(entry.getValue());

            if (endpoints.isEmpty()) {
                LOGGER.warn(
                        " -> Discovered button interaction bean {} but no endpoint could be extracted.",
                        entry.getKey()
                );
                continue;
            }

            LOGGER.info(
                    " -> Discovered button interaction bean {} with {} endpoint(s).",
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

}
