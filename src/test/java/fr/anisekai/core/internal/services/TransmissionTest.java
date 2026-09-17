package fr.anisekai.core.internal.services;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransmissionTest {

    private HttpServer server;
    private String     base;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {

        this.server   = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.base     = "http://127.0.0.1:" + this.server.getAddress().getPort();
        this.objectMapper = new ObjectMapper();
        this.server.start();
    }

    @AfterEach
    void tearDown() {

        this.server.stop(0);
    }

    @Test
    void getSessionReturnsSuccess() throws Exception {

        this.server.createContext("/", exchange -> {
            byte[] response = """
                    {"result":"success"}""".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        Transmission transmission = new Transmission(this.base);
        transmission.getSession();
    }

    @Test
    void queryReturnsTorrents() throws Exception {

        this.server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode request = objectMapper.readTree(body);
            assertEquals("torrent-get", request.get("method").asText());
            byte[] response = """
                    {"result":"success","arguments":{"torrents":[{"hashString":"abc","status":4,"downloadDir":"/tmp","percentDone":0.5,"files":[{"name":"test.torrent"}]}]}}""".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        Transmission transmission = new Transmission(this.base);
        List<Transmission.Torrent> torrents = transmission.query(List.of("abc"));

        assertEquals(1, torrents.size());
        assertEquals("abc", torrents.getFirst().hash());
        assertEquals("/tmp", torrents.getFirst().downloadDir());
    }

    @Test
     void downloadRetrievesAddedTorrent() throws Exception {

         this.server.createContext("/", exchange -> {
             String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
             JsonNode request = objectMapper.readTree(body);
             byte[] response;
             if (request.get("method").asText().equals("torrent-add")) {
                 response = """
                         {"result":"success","arguments":{"torrent-added":{"hashString":"abc"}}}""".getBytes(StandardCharsets.UTF_8);
             } else {
                 response = """
                         {"result":"success","arguments":{"torrents":[{"hashString":"abc","status":4,"downloadDir":"/tmp","percentDone":0.5,"files":[{"name":"test.torrent"}]}]}}""".getBytes(StandardCharsets.UTF_8);
             }
             exchange.sendResponseHeaders(200, response.length);
             exchange.getResponseBody().write(response);
             exchange.close();
         });

         Transmission transmission = new Transmission(this.base);
         Transmission.Torrent torrent = transmission.download(new Nyaa.Entry("title", "magnet:xxx", "http://example.torrent", null), true);

         assertEquals("abc", torrent.hash());
     }

    @Test
    void startThrowsOnFailure() throws Exception {

        this.server.createContext("/", exchange -> {
            byte[] response = """
                    {"result":"error"}""".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        Transmission transmission = new Transmission(this.base);
        Transmission.Torrent torrent = new Transmission.Torrent(UUID.randomUUID().toString(), Transmission.TorrentStatus.STOPPED, "/tmp", 0.0, List.of());

        assertThrows(IllegalStateException.class, () -> transmission.start(torrent));
    }

    @Test
    void deleteThrowsOnFailure() throws Exception {

        this.server.createContext("/", exchange -> {
            byte[] response = """
                    {"result":"error"}""".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        Transmission transmission = new Transmission(this.base);
        Transmission.Torrent torrent = new Transmission.Torrent(UUID.randomUUID().toString(), Transmission.TorrentStatus.STOPPED, "/tmp", 0.0, List.of());

        assertThrows(IllegalStateException.class, () -> transmission.delete(torrent));
    }

    @Test
    void queryThrowsOnHttpError() throws Exception {

        this.server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        Transmission transmission = new Transmission(this.base);

        HttpServerErrorException exception = assertThrows(
                HttpServerErrorException.class,
                () -> transmission.query(List.of("abc"))
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    void queryThrowsOnClientError() throws Exception {

        this.server.createContext("/", exchange -> {
            byte[] error = "not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, error.length);
            exchange.getResponseBody().write(error);
            exchange.close();
        });

        Transmission transmission = new Transmission(this.base);

        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> transmission.query(List.of("abc"))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void sessionRenewalOn409() throws Exception {

        AtomicReference<String> receivedSession = new AtomicReference<>();
        AtomicReference<String> secondRequestSession = new AtomicReference<>();

        this.server.createContext("/", exchange -> {
            String sessionHeader = exchange.getRequestHeaders().getFirst("X-Transmission-Session-Id");
            if (sessionHeader != null && !sessionHeader.isBlank()) {
                secondRequestSession.set(sessionHeader);
                exchange.sendResponseHeaders(200, """
                        {"result":"success"}""".getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write("""
                        {"result":"success"}""".getBytes(StandardCharsets.UTF_8));
            } else {
                receivedSession.set("first-session-id");
                exchange.getResponseHeaders().add("X-Transmission-Session-Id", "new-session-id");
                exchange.sendResponseHeaders(409, -1);
            }
            exchange.close();
        });

        Transmission transmission = new Transmission(this.base);
        transmission.getSession();

        assertEquals("new-session-id", secondRequestSession.get());
    }

}
