package fr.anisekai.discord.utils;

import fr.anisekai.core.internal.json.AnisekaiArray;
import fr.anisekai.core.internal.json.AnisekaiJson;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class EmbeddingUtils {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private EmbeddingUtils() {}

    public static AnisekaiJson toJson(MessageEmbed embed) {

        AnisekaiJson json = new AnisekaiJson();
        json.put("title", embed.getTitle());
        json.put("url", embed.getUrl());
        json.put("description", embed.getDescription());
        json.put("timestamp", Optional.ofNullable(embed.getTimestamp()).map(DTF::format).orElse(null));
        json.put("color", Optional.ofNullable(embed.getColor()).map(Color::getRGB).orElse(null));

        Optional.ofNullable(embed.getThumbnail()).ifPresent(thumbnail -> json.put("thumbnail.url", thumbnail.getUrl()));

        Optional.ofNullable(embed.getAuthor()).ifPresent(author -> {
            json.put("author.name", author.getName());
            json.put("author.url", author.getUrl());
            json.put("author.iconUrl", author.getIconUrl());
        });

        Optional.ofNullable(embed.getFooter()).ifPresent(footer -> {
            json.put("footer.text", footer.getText());
            json.put("footer.iconUrl", footer.getIconUrl());
        });

        Optional.ofNullable(embed.getImage()).ifPresent(image -> json.put("image.url", image.getUrl()));

        AnisekaiArray fields = new AnisekaiArray();
        for (MessageEmbed.Field field : embed.getFields()) {
            AnisekaiJson fieldJson = new AnisekaiJson();
            fieldJson.put("name", field.getName());
            fieldJson.put("value", field.getValue());
            fieldJson.put("inline", field.isInline());
            fields.put(fieldJson);
        }
        json.put("fields", fields);

        return json;
    }

    public static MessageEmbed fromJson(AnisekaiJson json) {

        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(json.readString("title"));
        builder.setUrl(json.readString("url"));
        builder.setDescription(json.readString("description"));
        builder.setTimestamp(json.getOptionalZonedDateTime("timestamp").orElse(null));
        builder.setColor(json.getOptionalInteger("color").orElse(Role.DEFAULT_COLOR_RAW));
        builder.setThumbnail(json.readString("thumbnail.url"));

        builder.setAuthor(
                json.readString("author.name"),
                json.readString("author.url"),
                json.readString("author.iconUrl")
        );

        builder.setFooter(json.readString("footer.text"), json.readString("footer.iconUrl"));
        builder.setImage(json.readString("image.url"));

        json.readList(
                    "fields",
                    fieldJson -> new MessageEmbed.Field(
                            fieldJson.readString("name"),
                            fieldJson.readString("value"),
                            json.getBoolean("inline"),
                            true
                    )
            )
            .forEach(builder::addField);

        return builder.build();
    }


}
