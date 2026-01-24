package fr.anisekai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Path;
import java.util.Base64;

@Configuration
@ConfigurationProperties(prefix = "anisekai")
public class ApplicationConfiguration {

    private final Api     api     = new Api();
    private final Library library = new Library();
    private final Discord discord = new Discord();

    public Api getApi() {

        return this.api;
    }

    public Library getLibrary() {

        return this.library;
    }

    public Discord getDiscord() {

        return this.discord;
    }

    public static class Api {

        private String signingKey;

        public String getSigningKey() {

            return this.signingKey;
        }

        public void setSigningKey(String signingKey) {

            this.signingKey = signingKey;
        }

        public SecretKey getSigningSecretKey() {

            byte[] decodedKey = Base64.getDecoder().decode(this.getSigningKey());
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
        }

    }

    public static class Library {

        private String path;

        public String getPath() {

            return this.path;
        }

        public void setPath(String path) {

            this.path = path;
        }

        public Path getIoPath() {

            return Path.of(this.path);
        }

    }

    public static class Discord {

        private final OAuth oauth = new OAuth();
        private final Bot   bot   = new Bot();

        public OAuth getOauth() {

            return this.oauth;
        }

        public Bot getBot() {

            return this.bot;
        }

        public static class OAuth {

            private String clientId;
            private String clientSecret;
            private String scope;
            private String redirectUri;

            public String getClientId() {

                return this.clientId;
            }

            public void setClientId(String clientId) {

                this.clientId = clientId;
            }

            public String getClientSecret() {

                return this.clientSecret;
            }

            public void setClientSecret(String clientSecret) {

                this.clientSecret = clientSecret;
            }

            public String getScope() {

                return this.scope;
            }

            public void setScope(String scope) {

                this.scope = scope;
            }

            public String getRedirectUri() {

                return this.redirectUri;
            }

            public void setRedirectUri(String redirectUri) {

                this.redirectUri = redirectUri;
            }

        }

        public static class Bot {

            private String  token;
            private boolean enabled;

            public String getToken() {

                return this.token;
            }

            public void setToken(String token) {

                this.token = token;
            }

            public boolean isEnabled() {

                return this.enabled;
            }

            public void setEnabled(boolean enabled) {

                this.enabled = enabled;
            }

        }

    }

}
