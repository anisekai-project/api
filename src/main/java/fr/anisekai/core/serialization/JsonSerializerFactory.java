package fr.anisekai.core.serialization;

import fr.anisekai.discord.tasks.Nothing;
import fr.anisekai.scheduler.commons.interfaces.ObjectSerializer;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class JsonSerializerFactory {

    private final ObjectMapper mapper;

    public JsonSerializerFactory(ObjectMapper mapper) {

        this.mapper = mapper;
    }

    public <T> ObjectSerializer<T> createSerializer(Class<T> clazz) {

        return new JacksonObjectSerializer<>(this.mapper, clazz);
    }

    public ObjectSerializer<Nothing> emptySerializer() {

        return new ObjectSerializer<>() {

            @Override
            public String serialize(Nothing container) {

                return "{}";
            }

            @Override
            public Nothing deserialize(String raw) {

                return Nothing.INSTANCE;
            }
        };
    }

}
