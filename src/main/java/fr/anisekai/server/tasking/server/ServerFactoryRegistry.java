package fr.anisekai.server.tasking.server;

import fr.anisekai.scheduler.tasking.exceptions.UnknownFactoryException;
import fr.anisekai.scheduler.tasking.interfaces.factories.FactoryRegistry;
import fr.anisekai.scheduler.tasking.interfaces.factories.ServerFactory;
import fr.anisekai.server.domain.entities.Task;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ServerFactoryRegistry implements FactoryRegistry<ServerFactory<Task, ?, ?>> {

    private final List<ServerFactory<Task, ?, ?>> factories;
    private final Map<String, ServerFactory<Task, ?, ?>> factoriesByName;

    public ServerFactoryRegistry(List<ServerFactory<Task, ?, ?>> factories) {

        this.factories = List.copyOf(factories);
        this.factoriesByName = factories.stream().collect(Collectors.toUnmodifiableMap(
                ServerFactory::getName,
                Function.identity()
        ));
    }

    @Override
    public @NonNull ServerFactory<Task, ?, ?> query(@NotNull String name) {

        ServerFactory<Task, ?, ?> factory = this.factoriesByName.get(name);
        if (factory == null) throw new UnknownFactoryException(name);
        return factory;
    }

    @Override
    public @NonNull <F extends ServerFactory<Task, ?, ?>> F query(@NotNull Class<F> factory) {

        return this.factories
                .stream()
                .filter(factory::isInstance)
                .map(factory::cast)
                .findAny()
                .orElseThrow(() -> new UnknownFactoryException(factory));
    }

    @Override
    public Collection<ServerFactory<Task, ?, ?>> getFactories() {

        return this.factories;
    }

}
