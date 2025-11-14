package fr.anisekai.core.proxy.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import fr.anisekai.proxy.interfaces.State;

import java.io.IOException;

public class JacksonStateSerializer extends JsonSerializer<State<?>> {

    @Override
    public void serialize(State<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        serializers.defaultSerializeValue(value.getInstance(), gen);
    }

}
