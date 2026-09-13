package fr.anisekai.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class IOUtils {

    private IOUtils() {}

    /**
     * Calculate the SHA-256 hash of the provided path.
     *
     * @param path
     *         The path pointing to the file to hash
     *
     * @return The hash string.
     */
    public static String hash(Path path) {

        if (!path.toFile().isFile()) {
            throw new IllegalArgumentException("Cannot calculate hash of a non-file path.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (
                    InputStream is = Files.newInputStream(path);
                    DigestInputStream dis = new DigestInputStream(is, digest)
            ) {
                dis.transferTo(OutputStream.nullOutputStream());
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hashing algorithm not available on this system", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the file for hashing: " + path, e);
        }
    }

}
