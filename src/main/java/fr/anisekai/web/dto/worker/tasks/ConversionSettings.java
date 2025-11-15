package fr.anisekai.web.dto.worker.tasks;

import fr.anisekai.media.enums.Codec;

public record ConversionSettings(Codec videoCodec, Codec audioCodec, Codec subtitleCodec) {

}
