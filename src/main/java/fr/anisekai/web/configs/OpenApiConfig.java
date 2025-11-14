package fr.anisekai.web.configs;

import fr.anisekai.server.domain.entities.SessionToken;
import fr.anisekai.web.annotations.RequireAuth;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OperationCustomizer customizeOperation() {

        return (operation, handlerMethod) -> {

            RequireAuth auth = handlerMethod.getMethodAnnotation(RequireAuth.class);
            if (auth == null) return operation;

            operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

            ApiResponses responses = operation.getResponses();

            responses.addApiResponse(
                    "401",
                    new ApiResponse().description("Unauthorized \u0080\u0093 missing or invalid authorization header")
            );

            responses.addApiResponse(
                    "403",
                    new ApiResponse()
                            .description(
                                    "Forbidden \u0080\u0093 not enough permissions (e.g., not admin, guest not allowed, etc.)")
            );
            StringBuilder desc = new StringBuilder("\u009F\u0094\u0090 **Authorization Required**\n\n");

            if (auth.requireAdmin()) {
                desc.append("\u0083 Requires **admin** privileges\n");
            }
            if (!auth.allowGuests()) {
                desc.append("\u0083 Guests are **not allowed**\n");
            }

            List<String> types = Arrays.stream(auth.allowedSessionTypes())
                                       .map(Enum::name)
                                       .toList();

            desc.append("\u0083 Allowed session types: `")
                .append(String.join(", ", types))
                .append("`\n");

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

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("anisekai-token")// Customize this
                ));
    }

}
