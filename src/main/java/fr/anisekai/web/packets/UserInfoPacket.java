package fr.anisekai.web.packets;

import fr.anisekai.web.packets.results.DiscordIdentity;
import fr.anisekai.web.packets.results.DiscordUserResponse;
import fr.anisekai.web.packets.results.UserToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class UserInfoPacket {

    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserToken token;
    private final String    userInfoUrl;

    public UserInfoPacket(UserToken token) {

        this(token, "https://discordapp.com/api/users/@me");
    }

    UserInfoPacket(UserToken token, String userInfoUrl) {

        this.token       = token;
        this.userInfoUrl = userInfoUrl;
    }

    public DiscordIdentity complete() throws Exception {

        return this.complete(CLIENT);
    }

    DiscordIdentity complete(HttpClient client) throws Exception {

        HttpRequest request = HttpRequest.newBuilder(URI.create(this.userInfoUrl))
                                         .header("User-Agent", "Bravediver/JavaSpring")
                                         .header("Accept-Language", "en-US,en;q=0.5")
                                         .header("Authorization", "Bearer " + this.token.getAccessToken())
                                         .GET()
                                         .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 204) {
            return null;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw httpError(response);
        }
        DiscordUserResponse userResponse = OBJECT_MAPPER.readValue(response.body(), DiscordUserResponse.class);
        return new DiscordIdentity(userResponse);
    }

    private static RuntimeException httpError(HttpResponse<byte[]> response) {

        HttpStatus statusCode = HttpStatus.valueOf(response.statusCode());
        if (statusCode.is4xxClientError()) {
            return new HttpClientErrorException(statusCode, "", httpHeaders(response), response.body(), StandardCharsets.UTF_8);
        }
        return new HttpServerErrorException(statusCode, "", httpHeaders(response), response.body(), StandardCharsets.UTF_8);
    }

    private static org.springframework.http.HttpHeaders httpHeaders(HttpResponse<byte[]> response) {

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        response.headers().map().forEach((key, values) -> headers.addAll(key, values));
        return headers;
    }

}
