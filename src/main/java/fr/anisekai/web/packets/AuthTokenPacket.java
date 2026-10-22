package fr.anisekai.web.packets;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.BuildInfo;
import fr.anisekai.web.packets.results.DiscordTokenResponse;
import fr.anisekai.web.packets.results.UserToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class AuthTokenPacket {

    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ApplicationConfiguration.Discord.OAuth configuration;
    private final String                                 code;
    private final String                                 tokenUrl;

    public AuthTokenPacket(ApplicationConfiguration.Discord.OAuth configuration, String code) {

        this(configuration, code, "https://discordapp.com/api/oauth2/token");
    }

    AuthTokenPacket(ApplicationConfiguration.Discord.OAuth configuration, String code, String tokenUrl) {

        this.configuration = configuration;
        this.code          = code;
        this.tokenUrl      = tokenUrl;
    }

    public UserToken complete() throws Exception {

        return this.complete(CLIENT);
    }

    UserToken complete(HttpClient client) throws Exception {

        String body = "client_id=%s&client_secret=%s&grant_type=authorization_code&code=%s&redirect_uri=%s&scope=%s".formatted(
                URLEncoder.encode(this.configuration.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(this.configuration.getClientSecret(), StandardCharsets.UTF_8),
                URLEncoder.encode(this.code, StandardCharsets.UTF_8),
                URLEncoder.encode(this.configuration.getRedirectUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(this.configuration.getScope(), StandardCharsets.UTF_8)
        );
        HttpRequest request = HttpRequest.newBuilder(URI.create(this.tokenUrl))
                                         .header("User-Agent", "anisekai/service " + BuildInfo.getVersion())
                                         .header("Accept-Language", "en-US,en;q=0.5")
                                         .header("Content-Type", "application/x-www-form-urlencoded")
                                         .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                         .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 204) {
            return null;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw httpError(response);
        }
        DiscordTokenResponse tokenResponse = OBJECT_MAPPER.readValue(response.body(), DiscordTokenResponse.class);
        return new UserToken(tokenResponse);
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
