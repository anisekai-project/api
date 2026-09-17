package fr.anisekai.web.packets;

import com.sun.net.httpserver.HttpServer;
import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.web.packets.results.DiscordIdentity;
import fr.anisekai.web.packets.results.DiscordTokenResponse;
import fr.anisekai.web.packets.results.DiscordUserResponse;
import fr.anisekai.web.packets.results.UserToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordPacketsTest {

    private HttpServer             server;
    private HttpClient             client;
    private String                 base;
    private AtomicReference<String> method;
    private AtomicReference<String> body;
    private AtomicReference<String> contentType;
    private AtomicReference<String> authHeader;

    @BeforeEach
    void setUp() throws Exception {

        this.server  = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.client  = HttpClient.newHttpClient();
        this.base    = "http://127.0.0.1:" + this.server.getAddress().getPort();
        this.method  = new AtomicReference<>();
        this.body    = new AtomicReference<>();
        this.contentType = new AtomicReference<>();
        this.authHeader = new AtomicReference<>();

        this.server.createContext("/token", exchange -> {
            this.method.set(exchange.getRequestMethod());
            this.body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            this.contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            if (this.body.get().contains("code=bad+code")) {
                byte[] error = "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, error.length);
                exchange.getResponseBody().write(error);
                exchange.close();
                return;
            }
            if (this.body.get().contains("code=nocontent")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            byte[] ok = """
                    {"access_token":"tok","token_type":"Bearer","expires_in":604800,"refresh_token":"ref","scope":"identify"}"""
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        this.server.createContext("/users/@me", exchange -> {
            this.method.set(exchange.getRequestMethod());
            this.authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            if (this.body.getAndSet("requested") != null) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            byte[] ok = """
                    {"id":"123456789012345678","username":"freya","global_name":"Freya","avatar":"abc"}"""
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        this.server.start();
    }

    @AfterEach
    void tearDown() {

        this.server.stop(0);
    }

    private ApplicationConfiguration.Discord.OAuth oauth() {

        ApplicationConfiguration.Discord.OAuth oauth = new ApplicationConfiguration.Discord.OAuth();
        oauth.setClientId("client id");
        oauth.setClientSecret("s3cr3t");
        oauth.setRedirectUri("https://anisekai.fr/auth");
        oauth.setScope("identify email");
        return oauth;
    }

    @Test
    void authTokenPacketPostsUrlEncodedBodyAndParsesToken() throws Exception {

        AuthTokenPacket packet = new AuthTokenPacket(oauth(), "code with spaces&more", this.base + "/token");
        UserToken       token  = packet.complete(this.client);

        assertEquals("POST", this.method.get());
        assertEquals("application/x-www-form-urlencoded", this.contentType.get());
        assertEquals(
                "client_id=client+id&client_secret=s3cr3t&grant_type=authorization_code&code=code+with+spaces%26more&redirect_uri=https%3A%2F%2Fanisekai.fr%2Fauth&scope=identify+email",
                this.body.get()
        );
        assertNotNull(token);
        assertEquals("tok", token.getAccessToken());
        assertEquals("ref", token.getRefreshToken());
    }

    @Test
    void authTokenPacketThrowsHttpClientErrorExceptionOnHttpError() {

        AuthTokenPacket packet = new AuthTokenPacket(oauth(), "bad code", this.base + "/token");

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> packet.complete(this.client));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getResponseBodyAsString().contains("invalid_grant"));
    }

    @Test
    void authTokenPacketReturnsNullOnNoContent() throws Exception {

        AuthTokenPacket packet = new AuthTokenPacket(oauth(), "nocontent", this.base + "/token");

        assertNull(packet.complete(this.client));
    }

    @Test
    void userInfoPacketSendsBearerAndParsesIdentity() throws Exception {

        DiscordTokenResponse tokenResponse = new DiscordTokenResponse("tok", "Bearer", 604800, "ref", "identify");
        UserToken userToken = new UserToken(tokenResponse);
        UserInfoPacket packet = new UserInfoPacket(userToken, this.base + "/users/@me");

        DiscordIdentity identity = packet.complete(this.client);

        assertEquals("GET", this.method.get());
        assertEquals("Bearer tok", this.authHeader.get());
        assertEquals("freya", identity.getUsername());
    }

    @Test
    void userInfoPacketReturnsNullOnNoContent() throws Exception {

        this.body.set("primed");
        DiscordTokenResponse tokenResponse = new DiscordTokenResponse("tok", "Bearer", 604800, "ref", "identify");
        UserToken userToken = new UserToken(tokenResponse);
        UserInfoPacket packet = new UserInfoPacket(userToken, this.base + "/users/@me");

        assertNull(packet.complete(this.client));
    }

}
