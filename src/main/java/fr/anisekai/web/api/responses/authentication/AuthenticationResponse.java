package fr.anisekai.web.api.responses.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

@Schema(description = "Structure of an authentication response.")
public record AuthenticationResponse(
        @Schema(
                description = "The JWT session token to use as 'anisekai-access-token' cookie or authorization bearer.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBbmlzZWthaSIsInN1YiI6IjE0OTI3OTE1MDY0ODA2NjA0OCIsImV4cCI6MTc3MTc4MjM2MCwibmJmIjoxNzcxNzY3OTYwLCJpYXQiOjE3NzE3Njc5NjAsImp0aSI6IjAxOWM4NTk5LTk1NWEtNzQ2Zi1hYWY5LWI2NGU0NTJmOTFjOSIsInJvbGUiOiJVU0VSIn0.rnKGqaBAlBeJ8SC_f2xAw_zQEbqXxHQH2pfkOw6HUDs"
        ) @NotNull String accessToken,
        @Schema(
                description = "The JWT refresh token to use when the access token is expired.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBbmlzZWthaSIsInN1YiI6IjE0OTI3OTE1MDY0ODA2NjA0OCIsImV4cCI6MTc3MjM3Mjc2MCwibmJmIjoxNzcxNzY3OTYwLCJpYXQiOjE3NzE3Njc5NjAsImp0aSI6IjAxOWM4NTk5LTk1OTQtN2I3Ni05YjFiLTM5ZDIwYjdiYjBkOCIsInJvbGUiOiJSRUZSRVNIIn0.PoIUBr5u8BtGX5UjOZ0UDKSXF1A3OTtlEr2D2b8DB4s"
        ) @NotNull String refreshToken
) {

}
