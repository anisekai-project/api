package fr.anisekai.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUrlStreamer {

    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private final Path   file;
    private final String url;

    public FileUrlStreamer(Path file, String url) {

        this.file = file;
        this.url  = url;

        if (Files.isDirectory(file)) {
            throw new IllegalArgumentException(file + " is a directory");
        }
    }

    public Boolean complete() throws Exception {

        HttpRequest request = HttpRequest.newBuilder(URI.create(this.url)).GET().build();
        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream input = response.body()) {
            if (response.statusCode() == 204) {
                return false;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw httpError(response, input);
            }
            try (OutputStream output = Files.newOutputStream(this.file)) {
                byte[] buffer = new byte[8192];
                int    bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            return true;
        }
    }

    private static RuntimeException httpError(HttpResponse<InputStream> response, InputStream body) {

        HttpStatus statusCode = HttpStatus.valueOf(response.statusCode());
        byte[] bodyBytes;
        try {
            bodyBytes = body.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (statusCode.is4xxClientError()) {
            return new HttpClientErrorException(statusCode, "", httpHeaders(response), bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
        }
        return new HttpServerErrorException(statusCode, "", httpHeaders(response), bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static org.springframework.http.HttpHeaders httpHeaders(HttpResponse<?> response) {

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        response.headers().map().forEach((key, values) -> headers.addAll(key, values));
        return headers;
    }

}
