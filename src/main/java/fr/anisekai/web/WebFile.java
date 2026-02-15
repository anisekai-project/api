package fr.anisekai.web;

import fr.anisekai.library.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public final class WebFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFile.class);

    private final Library library;

    public WebFile(Library library) {

        this.library = library;
    }

    public ResponseEntity<InputStreamResource> serve(Path path, MediaType type) {

        return this.serve(path, type, null);
    }

    public ResponseEntity<InputStreamResource> serve(Path path, MediaType type, String filename) {

        String filePathLog = this.library.relativize(path).toString();

        if (!Files.exists(path)) {
            LOGGER.warn("Could not serve '{}': The file was not found.", filePathLog);
            return ResponseEntity.notFound().build();
        }
        if (!Files.isRegularFile(path)) {
            LOGGER.warn("Could not serve '{}': The path does not lead to a file.", filePathLog);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        }

        InputStreamResource resource;
        long                contentLength;

        try {
            resource      = new InputStreamResource(Files.newInputStream(path));
            contentLength = Files.size(path);
        } catch (IOException e) {
            LOGGER.error("Could not serve file '{}': Encountered exception when opening the file.", filePathLog, e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        }
        ContentDisposition.Builder contentDisposition = ContentDisposition.inline();

        if (filename != null) {
            contentDisposition = contentDisposition.filename(filename, StandardCharsets.UTF_8);
        }

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.build().toString())
                             .contentLength(contentLength)
                             .contentType(type)
                             .body(resource);
    }

}
