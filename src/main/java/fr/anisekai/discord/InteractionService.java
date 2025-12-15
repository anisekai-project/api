package fr.anisekai.discord;

import fr.alexpado.interactions.InteractionManager;
import fr.alexpado.interactions.interfaces.routing.Interceptor;
import fr.alexpado.interactions.providers.interactions.button.ButtonSchemeAdapter;
import fr.alexpado.interactions.providers.interactions.slash.adapters.AutocompleteSchemeAdapter;
import fr.alexpado.interactions.providers.interactions.slash.adapters.SlashSchemeAdapter;
import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.BuildInfo;
import fr.anisekai.discord.resolvers.ButtonInteractionResolver;
import fr.anisekai.discord.resolvers.SlashInteractionResolver;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.discord.responses.DiscordResponseHandler;
import fr.anisekai.utils.ReflectionUtils;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class InteractionService extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractionService.class);

    private final InteractionManager       manager;
    private final ApplicationConfiguration configuration;
    private final ListableBeanFactory      factory;

    private final SlashInteractionResolver slashResolver;

    public InteractionService(
            SlashInteractionResolver slashResolver,
            ButtonInteractionResolver buttonResolver,
            List<Interceptor> interceptors,
            ApplicationConfiguration configuration,
            ListableBeanFactory factory
    ) {

        this.factory = factory;
        this.manager = new InteractionManager();

        this.manager.getRouter().registerResolver(slashResolver);
        this.manager.getRouter().registerResolver(buttonResolver);

        this.manager.registerAdapter(SlashCommandInteraction.class, new SlashSchemeAdapter());
        this.manager.registerAdapter(ButtonInteraction.class, new ButtonSchemeAdapter());
        this.manager.registerAdapter(CommandAutoCompleteInteraction.class, new AutocompleteSchemeAdapter());

        this.manager.setErrorHandler(new InteractionErrorHandler());
        this.manager.getResponseManager().registerHandler(DiscordResponse.class, new DiscordResponseHandler());

        LOGGER.info("Found {} interceptors.", interceptors.size());
        interceptors.stream()
                    .sorted(ReflectionUtils.compareOrder())
                    .forEach(interceptor -> {
                        LOGGER.debug(" -> Registering {} interceptor...", interceptor.getClass().getSimpleName());
                        this.manager.getRouter().registerInterceptor(interceptor);
                    });

        this.configuration = configuration;
        this.slashResolver = slashResolver;
    }

    private boolean canLogin(ApplicationConfiguration.Discord.Bot bot) {

        if (!bot.isEnabled()) {
            LOGGER.info("Ignoring discord bot startup: The bot has been disabled.");
            return false;
        }

        if (bot.getToken().isBlank()) {
            LOGGER.warn("Ignoring discord bot startup: The bot is enabled but no token was provided.");
            return false;
        }

        return true;
    }

    public void login() {

        ApplicationConfiguration.Discord     discord = this.configuration.getDiscord();
        ApplicationConfiguration.Discord.Bot bot     = discord.getBot();

        if (!this.canLogin(bot)) return;

        JDABuilder builder = JDABuilder.create(bot.getToken(), GatewayIntent.getIntents(GatewayIntent.DEFAULT));

        LOGGER.info("Registering JDA listeners...");
        Arrays.stream(this.factory.getBeanNamesForType(ListenerAdapter.class))
              .map(name -> this.factory.getBean(name, ListenerAdapter.class))
              .forEach(listener -> {
                  LOGGER.debug(" -> {}", listener.getClass().getSimpleName());
                  builder.addEventListeners(listener);
              });

        try {
            LOGGER.info("Starting up JDA...");
            builder.build();
        } catch (Exception e) {
            LOGGER.error("Unable to start JDA", e);
            Sentry.withScope(scope -> {
                scope.setLevel(SentryLevel.FATAL);
                Sentry.captureException(e);
            });
        }
    }

    @Override
    public void onReady(ReadyEvent event) {

        LOGGER.info("Setting app version...");
        event.getJDA().getPresence().setPresence(Activity.customStatus(BuildInfo.getVersion()), false);

        LOGGER.info("Registering commands....");
        Set<CommandData> commands = this.slashResolver.getJdaCommands();
        event.getJDA().updateCommands().addCommands(commands).complete();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        this.manager.processEvent(event.getInteraction());
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        this.manager.processEvent(event.getInteraction());
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {

        this.manager.processEvent(event.getInteraction());
    }

}
