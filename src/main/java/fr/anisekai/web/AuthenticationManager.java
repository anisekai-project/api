package fr.anisekai.web;

import com.github.f4b6a3.uuid.UuidCreator;
import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.server.repositories.SessionTokenRepository;
import fr.anisekai.server.services.UserService;
import fr.anisekai.web.dto.auth.AuthData;
import fr.anisekai.web.enums.TokenType;
import fr.anisekai.web.exceptions.auth.BearerParsingException;
import fr.anisekai.web.exceptions.auth.InvalidSessionException;
import fr.anisekai.web.exceptions.auth.TokenExpiredException;
import fr.anisekai.web.exceptions.auth.TokenRevokedException;
import fr.anisekai.web.packets.AuthTokenPacket;
import fr.anisekai.web.packets.UserInfoPacket;
import fr.anisekai.web.packets.results.DiscordIdentity;
import fr.anisekai.web.packets.results.UserToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthenticationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationManager.class);

    private static final Duration ACCESS_TOKEN_LIFETIME  = Duration.of(4, ChronoUnit.HOURS);
    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.of(7, ChronoUnit.DAYS);

    private final ApplicationConfiguration.Discord.OAuth oauthConfiguration;
    private final ApplicationConfiguration.Api           apiConfiguration;
    private final UserService                            userService;
    private final SessionTokenRepository                 sessionTokenRepository;

    public AuthenticationManager(ApplicationConfiguration configuration, UserService userService, SessionTokenRepository sessionTokenRepository) {

        this.oauthConfiguration     = configuration.getDiscord().getOauth();
        this.apiConfiguration       = configuration.getApi();
        this.userService            = userService;
        this.sessionTokenRepository = sessionTokenRepository;
    }

    private SessionToken createToken(TokenType type, DiscordUser user, Instant expiresAt) {

        SessionToken token = new SessionToken();
        token.setId(UuidCreator.getTimeOrderedEpoch());
        token.setOwner(user);
        token.setType(type);
        token.setExpiresAt(expiresAt);

        // Ensure we have the 'entity' version
        return this.sessionTokenRepository.save(token);
    }

    /**
     * Find a {@link SessionToken} matching the provided {@link UUID} being one of the given {@link TokenType}.
     *
     * @param uuid
     *         The {@link UUID} of the {@link SessionToken}.
     * @param types
     *         The {@link TokenType} allowed for the {@link SessionToken} matching.
     *
     * @return The matching {@link SessionToken}
     *
     * @throws InvalidSessionException
     *         when no {@link SessionToken} matched the {@link UUID} and {@link TokenType}.
     * @throws TokenExpiredException
     *         when {@link SessionToken#getExpiresAt()} is in the past.
     * @throws TokenRevokedException
     *         when {@link SessionToken#getRevokedAt()} is set and in the past.
     */
    public SessionToken findToken(UUID uuid, TokenType... types) {

        SessionToken token = this.sessionTokenRepository
                .findByIdAndTypeIn(uuid, Arrays.asList(types))
                .orElseThrow(() -> new InvalidSessionException(uuid));

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        if (token.getExpiresAt().isBefore(now)) {
            throw new TokenExpiredException(uuid);
        }

        if (token.getRevokedAt() != null && token.getRevokedAt().isBefore(now)) {
            throw new TokenRevokedException(uuid);
        }

        return token;
    }

    /**
     * Retrieve the {@link UUID} used as JTI claim from the provided JWT.
     *
     * @param jwt
     *         The JWT to parse.
     *
     * @return The {@link UUID}.
     *
     * @throws BearerParsingException
     *         when the {@link UUID} or JWT could not be parsed.
     */
    public UUID getJti(CharSequence jwt) {

        String jti;
        try {
            Jws<Claims> jws = Jwts.parser()
                                  .verifyWith(this.apiConfiguration.getSigningSecretKey()) // Provide the same key used for signing
                                  .build().parseSignedClaims(jwt);

            Claims claims = jws.getPayload();
            jti = claims.getId();
        } catch (JwtException e) {
            throw new BearerParsingException(e);
        }

        if (jti == null || jti.isEmpty()) {
            throw new BearerParsingException("No JTI claim");
        }

        try {
            return UUID.fromString(jti);
        } catch (IllegalArgumentException e) {
            throw new BearerParsingException(e);
        }
    }

    /**
     * Retrieve a {@link SessionToken} used to access the API. The {@link SessionToken} can be either a user token
     * ({@link TokenType#USER}) or an application key ({@link TokenType#APPLICATION}).
     * <p>
     * This is the same as calling {@link #findToken(UUID, TokenType...)} with proper {@link TokenType}. This method
     * only exists for convenience and clear intent.
     *
     * @param uuid
     *         The {@link UUID} of the {@link SessionToken}.
     *
     * @return The matching {@link SessionToken}
     *
     * @throws InvalidSessionException
     *         when no {@link SessionToken} matched the {@link UUID} and {@link TokenType}.
     * @throws TokenExpiredException
     *         when {@link SessionToken#getExpiresAt()} is in the past.
     * @throws TokenRevokedException
     *         when {@link SessionToken#getRevokedAt()} is set and in the past.
     */
    public SessionToken getAccessToken(UUID uuid) {

        return this.findToken(uuid, TokenType.USER, TokenType.APPLICATION);
    }

    /**
     * Retrieve a {@link SessionToken} used to refresh an access token.
     * <p>
     * This is the same as calling {@link #findToken(UUID, TokenType...)} with proper {@link TokenType}. This method
     * only exists for convenience and clear intent.
     *
     * @param uuid
     *         The {@link UUID} of the {@link SessionToken}.
     *
     * @return The matching {@link SessionToken}
     *
     * @throws InvalidSessionException
     *         when no {@link SessionToken} matched the {@link UUID} and {@link TokenType}.
     * @throws TokenExpiredException
     *         when {@link SessionToken#getExpiresAt()} is in the past.
     * @throws TokenRevokedException
     *         when {@link SessionToken#getRevokedAt()} is set and in the past.
     */
    public SessionToken getRefreshToken(UUID uuid) {

        return this.findToken(uuid, TokenType.REFRESH);
    }

    /**
     * Create a {@link SessionToken} with a type of {@link TokenType#USER}
     *
     * @param user
     *         The {@link DiscordUser} for which the {@link SessionToken} will be created.
     *
     * @return The newly created {@link SessionToken}.
     */
    public SessionToken createAccessToken(DiscordUser user) {

        return this.createToken(TokenType.USER, user, Instant.now().plus(ACCESS_TOKEN_LIFETIME));
    }

    /**
     * Create a {@link SessionToken} with a type of {@link TokenType#REFRESH}
     *
     * @param user
     *         The {@link DiscordUser} for which the {@link SessionToken} will be created.
     *
     * @return The newly created {@link SessionToken}.
     */
    public SessionToken createRefreshToken(DiscordUser user) {

        return this.createToken(TokenType.REFRESH, user, Instant.now().plus(REFRESH_TOKEN_LIFETIME));
    }

    /**
     * Create a {@link SessionToken} with a type of {@link TokenType#APPLICATION}
     *
     * @param user
     *         The {@link DiscordUser} for which the {@link SessionToken} will be created.
     *
     * @return The newly created {@link SessionToken}.
     */
    public SessionToken createApplicationToken(DiscordUser user, Instant expiresAt) {

        return this.createToken(TokenType.APPLICATION, user, expiresAt);
    }

    public AuthData exchange(String code) throws Exception {

        LOGGER.info("Authentication request using {}", code);
        AuthTokenPacket authTokenPacket = new AuthTokenPacket(this.oauthConfiguration, code);
        UserToken       userToken       = authTokenPacket.complete();

        LOGGER.debug("Authentication successful. Requesting user identity...");
        UserInfoPacket  userInfoPacket = new UserInfoPacket(userToken);
        DiscordIdentity identity       = userInfoPacket.complete();

        LOGGER.info("Welcome, {} ! ({})", identity.getUsername(), identity.getId());
        DiscordUser user = this.userService.ensureUserExists(identity);

        SessionToken accessToken  = this.createAccessToken(user);
        SessionToken refreshToken = this.createRefreshToken(user);

        return new AuthData(accessToken, refreshToken);
    }

    public AuthData refresh(String jwt) {

        UUID         uuid  = this.getJti(jwt);
        SessionToken token = this.getRefreshToken(uuid);

        SessionToken accessToken  = this.createAccessToken(token.getOwner());
        SessionToken refreshToken = this.createRefreshToken(token.getOwner());

        token.setRevokedAt(refreshToken.getCreatedAt());
        this.sessionTokenRepository.save(token);

        return new AuthData(accessToken, refreshToken);
    }

    public String stringify(SessionToken token) {

        return Jwts
                .builder()
                // 1. ISS
                .issuer("Anisekai")
                // 2. SUB
                .subject(String.valueOf(token.getOwner().getId()))
                // 3. EXP
                .expiration(Date.from(token.getExpiresAt()))
                // 4. NBF
                .notBefore(Date.from(token.getCreatedAt()))
                // 5. IAT
                .issuedAt(Date.from(token.getCreatedAt()))
                // 6. JTI
                .id(token.getId().toString())
                // 7. Role Claim
                .claim("role", token.getType().name())
                .signWith(this.apiConfiguration.getSigningSecretKey())
                .compact();

    }

}
