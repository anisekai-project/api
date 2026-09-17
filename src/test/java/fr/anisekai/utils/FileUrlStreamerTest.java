package fr.anisekai.utils;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUrlStreamerTest {

    private HttpServer server;
    private String     base;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {

        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.base   = "http://127.0.0.1:" + this.server.getAddress().getPort();
        this.server.start();
    }

    @AfterEach
    void tearDown() {

        this.server.stop(0);
    }

    @Test
    void streamsSuccessfulResponseToFile() throws Exception {

        byte[] payload = "A".repeat(20000).getBytes(StandardCharsets.UTF_8);
        this.server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        Path target = this.tempDir.resolve("out.bin");

        Boolean result = new FileUrlStreamer(target, this.base + "/file").complete();

        assertTrue(result);
        assertArrayEquals(payload, Files.readAllBytes(target));
    }

    @Test
    void returnsFalseOnNoContent() throws Exception {

        this.server.createContext("/empty", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        Path target = this.tempDir.resolve("out.bin");

        Boolean result = new FileUrlStreamer(target, this.base + "/empty").complete();

        assertFalse(result);
        assertFalse(Files.exists(target));
    }

    @Test
    void throwsHttpClientErrorExceptionOnClientError() {

        this.server.createContext("/missing", exchange -> {
            byte[] error = "{\"error\":\"not_found\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, error.length);
            exchange.getResponseBody().write(error);
            exchange.close();
        });
        Path target = this.tempDir.resolve("out.bin");

        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> new FileUrlStreamer(target, this.base + "/missing").complete()
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getResponseBodyAsString().contains("not_found"));
    }

    @Test
    void throwsHttpServerErrorExceptionOnServerError() {

        this.server.createContext("/error", exchange -> {
            byte[] error = "internal error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, error.length);
            exchange.getResponseBody().write(error);
            exchange.close();
        });
        Path target = this.tempDir.resolve("out.bin");

        HttpServerErrorException exception = assertThrows(
                HttpServerErrorException.class,
                () -> new FileUrlStreamer(target, this.base + "/error").complete()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

}
