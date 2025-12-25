package fr.anisekai.web.configs;

import fr.anisekai.ApplicationConfiguration;
import fr.anisekai.web.interceptors.AuthenticationInterceptor;
import fr.anisekai.web.interceptors.IsolationInterceptor;
import fr.anisekai.web.resolvers.IsolationArgumentResolver;
import fr.anisekai.web.resolvers.SessionArgumentResolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebConfig.class);

    private final SessionArgumentResolver      sessionArgumentResolver;
    private final IsolationArgumentResolver    isolationArgumentResolver;
    private final AuthenticationInterceptor    authenticationInterceptor;
    private final IsolationInterceptor         isolationInterceptor;
    private final ApplicationConfiguration.Api config;

    public WebConfig(SessionArgumentResolver sessionArgumentResolver, IsolationArgumentResolver isolationArgumentResolver, AuthenticationInterceptor authenticationInterceptor, IsolationInterceptor isolationInterceptor, ApplicationConfiguration config) {

        this.sessionArgumentResolver   = sessionArgumentResolver;
        this.isolationArgumentResolver = isolationArgumentResolver;
        this.authenticationInterceptor = authenticationInterceptor;
        this.isolationInterceptor      = isolationInterceptor;
        this.config                    = config.getApi();
    }

    @Bean
    public RestOperations restOperations() {

        return new RestTemplate();
    }

    @Override
    public void addCorsMappings(@NotNull CorsRegistry registry) {

        LOGGER.info("CORS: Setting allowed hosts to {},{}", this.config.getWebUrl(), this.config.getAllowedHost());

        registry.addMapping("/**") // Allow all endpoints
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PATCH", "DELETE") // GET, POST, etc.
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(this.authenticationInterceptor)
                .addPathPatterns("/api/v3/**");

        registry.addInterceptor(this.isolationInterceptor)
                .addPathPatterns("/api/v3/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {

        resolvers.add(this.sessionArgumentResolver);
        resolvers.add(this.isolationArgumentResolver);
    }

}
