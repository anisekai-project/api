package fr.anisekai.server.tasking.client;

import fr.anisekai.scheduler.tasking.exceptions.UnknownFactoryException;
import fr.anisekai.scheduler.tasking.interfaces.factories.ClientFactory;
import fr.anisekai.scheduler.tasking.interfaces.factories.FactoryRegistry;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ClientFactoryRegistry implements FactoryRegistry<ClientFactory<?, ?>> {

    private final List<ClientFactory<?, ?>>        factories;
    private final Map<String, ClientFactory<?, ?>> factoriesByName;

    public ClientFactoryRegistry(List<ClientFactory<?, ?>> factories) {

        this.factories       = List.copyOf(factories);
        this.factoriesByName = factories.stream().collect(Collectors.toUnmodifiableMap(
                ClientFactory::getName,
                Function.identity()
        ));
    }

    @Override
    public @NonNull ClientFactory<?, ?> query(@NotNull String name) {

        ClientFactory<?, ?> factory = this.factoriesByName.get(name);
        if (factory == null) throw new UnknownFactoryException(name);
        return factory;
    }

    @Override
    public @NonNull <F extends ClientFactory<?, ?>> F query(@NotNull Class<F> factory) {

        return this.factories
                .stream()
                .filter(factory::isInstance)
                .map(factory::cast)
                .findAny()
                .orElseThrow(() -> new UnknownFactoryException(factory));
    }

    @Override
    public Collection<ClientFactory<?, ?>> getFactories() {

        return this.factories;
    }

}
