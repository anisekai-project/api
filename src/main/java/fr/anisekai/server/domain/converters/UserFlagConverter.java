package fr.anisekai.server.domain.converters;

import fr.anisekai.server.domain.enums.UserFlag;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.EnumSet;

@Converter
public class UserFlagConverter implements AttributeConverter<EnumSet<UserFlag>, Integer> {

    @Override
    public Integer convertToDatabaseColumn(EnumSet<UserFlag> attribute) {

        if (attribute == null) {
            return 0;
        }
        return attribute.stream()
                        .mapToInt(UserFlag::getValue)
                        .reduce(0, (a, b) -> a | b);
    }

    @Override
    public EnumSet<UserFlag> convertToEntityAttribute(Integer dbData) {

        if (dbData == null) {
            return EnumSet.noneOf(UserFlag.class);
        }
        EnumSet<UserFlag> flags = EnumSet.noneOf(UserFlag.class);
        for (UserFlag flag : UserFlag.values()) {
            if ((dbData & flag.getValue()) == flag.getValue()) {
                flags.add(flag);
            }
        }
        return flags;
    }

}
