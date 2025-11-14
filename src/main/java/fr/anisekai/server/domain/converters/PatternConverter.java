package fr.anisekai.server.domain.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.regex.Pattern;

@Converter
public class PatternConverter implements AttributeConverter<Pattern, String> {

    @Override
    public String convertToDatabaseColumn(Pattern pattern) {

        return pattern != null ? pattern.pattern() : null;
    }

    @Override
    public Pattern convertToEntityAttribute(String string) {

        return string != null ? Pattern.compile(string) : null;
    }

}
