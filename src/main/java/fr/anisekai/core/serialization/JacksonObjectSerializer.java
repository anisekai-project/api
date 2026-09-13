package fr.anisekai.core.serialization;

import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import tools.jackson.databind.ObjectMapper;

public class JacksonObjectSerializer<T> implements ObjectSerializer<T> {

    private final ObjectMapper mapper;
    private final Class<T>     type;

    public JacksonObjectSerializer(ObjectMapper mapper, Class<T> type) {

        this.mapper = mapper;
        this.type   = type;
    }

    @Override
    public String serialize(T container) {

        return this.mapper.writeValueAsString(container);
    }

    @Override
    public T deserialize(String raw) {

        return this.mapper.readValue(raw, this.type);
    }

}
