package fr.anisekai.web.packets.results;

public class UserToken {

    private String accessToken;
    private String tokenType;
    private int    expiresIn;
    private String refreshToken;
    private String scope;
    private long   generationTime;
    private long   lastActivity;

    public UserToken(DiscordTokenResponse response) {

        this.load(response);
        this.signalActivity();
    }

    public void load(DiscordTokenResponse response) {

        this.accessToken    = response.accessToken();
        this.tokenType      = response.tokenType();
        this.expiresIn      = response.expiresIn();
        this.refreshToken   = response.refreshToken();
        this.scope          = response.scope();
        this.generationTime = System.currentTimeMillis() / 1000;
    }

    public String getAccessToken() {

        return this.accessToken;
    }

    public String getTokenType() {

        return this.tokenType;
    }

    public int getExpiresIn() {

        return this.expiresIn;
    }

    public String getRefreshToken() {

        return this.refreshToken;
    }

    public String getScope() {

        return this.scope;
    }

    public long getGenerationTime() {

        return this.generationTime;
    }

    public long getLastActivity() {

        return this.lastActivity;
    }

    public void signalActivity() {

        this.lastActivity = System.currentTimeMillis() / 1000;
    }

    public boolean isExpired() {

        return System.currentTimeMillis() / 1000 > this.generationTime + this.expiresIn;
    }

    public boolean willExpire(long shift) {

        return System.currentTimeMillis() / 1000 > (this.generationTime + this.expiresIn - shift);
    }

    public boolean isInactive(long ttl) {

        return System.currentTimeMillis() / 1000 > this.lastActivity + ttl;
    }

    @Override
    public String toString() {

        return "UserToken{accessToken='%s', tokenType='%s', expiresIn=%d, refreshToken='%s', scope='%s'}".formatted(
                this.accessToken,
                this.tokenType,
                this.expiresIn,
                this.refreshToken,
                this.scope
        );
    }

}
