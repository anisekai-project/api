package fr.anisekai.core.proxy.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.anisekai.proxy.interfaces.State;

public class JacksonStateSerializerModule extends SimpleModule {

    public JacksonStateSerializerModule() {

        super("JacksonStateSerializerModule");

        this.addStateSerializer();
    }

    // This helper method uses a generic parameter to satisfy the compiler.
    private <T> void addStateSerializer() {

        @SuppressWarnings("unchecked")
        Class<State<T>> stateClass = (Class<State<T>>) (Class<?>) State.class;

        this.addSerializer(stateClass, new JacksonStateSerializer());
    }

}
