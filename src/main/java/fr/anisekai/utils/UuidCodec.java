package fr.anisekai.utils;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public final class UuidCodec {

    public static final int ENCODED_LENGTH = 22;

    private UuidCodec() {

    }

    public static @NotNull String encode(@NotNull UUID uuid) {

        byte[] bytes = ByteBuffer.allocate(16)
                                 .putLong(uuid.getMostSignificantBits())
                                 .putLong(uuid.getLeastSignificantBits())
                                 .array();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static @NotNull UUID decode(@NotNull String value) {

        if (value.length() != ENCODED_LENGTH || value.indexOf('=') >= 0) {
            throw new IllegalArgumentException("Expected an unpadded Base64URL UUID");
        }

        byte[] bytes = Base64.getUrlDecoder().decode(value);
        if (bytes.length != 16 || !encode(fromBytes(bytes)).equals(value)) {
            throw new IllegalArgumentException("Expected a canonical Base64URL UUID");
        }
        return fromBytes(bytes);
    }

    private static UUID fromBytes(byte[] bytes) {

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

}
