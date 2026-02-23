package fr.anisekai.web.configs;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.annotations.RequireAuth;
import fr.anisekai.web.exceptions.WebException;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static Content createErrorContent(int status) {

        Map<String, Object> exampleValues = new LinkedHashMap<>();
        exampleValues.put("timestamp", ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        exampleValues.put("status", status);
        exampleValues.put("message", "An explanation message about the error here.");

        return new Content().addMediaType(
                "application/json",
                new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/WebException"))
                        .example(exampleValues)
        );
    }

    @Bean
    public OperationCustomizer customizeOperation() {

        return (operation, handlerMethod) -> {

            RequireAuth auth = handlerMethod.getMethodAnnotation(RequireAuth.class);
            if (auth == null) return operation;

            operation.setSecurity(List.of(
                    new SecurityRequirement().addList("bearerAuth"),
                    new SecurityRequirement().addList("cookieAuth")
            ));

            ApiResponses responses = operation.getResponses();

            responses.addApiResponse(
                    "401",
                    new ApiResponse()
                            .description("Unauthorized – missing or invalid authorization header/cookie")
                            .content(createErrorContent(401))
            );

            responses.addApiResponse(
                    "403", new ApiResponse()
                            .description("Forbidden – not enough permissions (e.g., not admin, guest not allowed, etc.)")
                            .content(createErrorContent(403))
            );

            responses.addApiResponse(
                    "500", new ApiResponse()
                            .description("Internal Server Error – Something went very wrong with the server.")
                            .content(createErrorContent(500))
            );

            StringBuilder desc = new StringBuilder("**Authorization Required**\n\n");

            if (auth.requireAdmin()) {
                desc.append("Requires **admin** privileges\n\n");
            }
            if (!auth.allowGuests()) {
                desc.append("Guests are **not allowed**\n\n");
            }

            List<String> types = Arrays.stream(auth.allowedSessionTypes())
                                       .map(Enum::name)
                                       .toList();

            List<String> scopes = Arrays.stream(auth.scopes())
                                        .map(Enum::name)
                                        .toList();


            desc.append("Allowed session types: `")
                .append(String.join(", ", types))
                .append("`\n\n");

            if (!scopes.isEmpty()) {
                desc.append("Required token scopes: `")
                    .append(String.join(", ", scopes))
                    .append("`\n\n");
            }

            operation.setDescription(desc.toString());

            return operation;
        };
    }


    @Bean
    public ParameterCustomizer sessionParameterHider() {

        return (parameterModel, methodParameter) -> {
            if (SessionToken.class.isAssignableFrom(methodParameter.getParameterType())) {
                return null;
            }
            return parameterModel;
        };
    }

    @Bean
    public OpenAPI customOpenAPI() {

        var schemaMap = ModelConverters.getInstance().read(WebException.Dto.class);

        return new OpenAPI()
                .components(new Components()
                                    .addSchemas("WebException", schemaMap.get("Dto"))

                                    .addSecuritySchemes(
                                            "bearerAuth", new SecurityScheme()
                                                    .name("Authorization")
                                                    .type(SecurityScheme.Type.HTTP)
                                                    .scheme("bearer")
                                                    .bearerFormat("anisekai-token")
                                    )

                                    .addSecuritySchemes(
                                            "cookieAuth", new SecurityScheme()
                                                    .name("anisekai-access-token") // The cookie name
                                                    .type(SecurityScheme.Type.APIKEY)
                                                    .in(SecurityScheme.In.COOKIE)
                                    )
                );
    }

}
