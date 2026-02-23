package fr.anisekai.web.api.requests.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

@Schema(description = "Structure of a session refresh request.")
public record RefreshRequest(
        @Schema(
                description = "The JWT refresh token given when the user authenticated.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBbmlzZWthaSIsInN1YiI6IjE0OTI3OTE1MDY0ODA2NjA0OCIsImV4cCI6MTc3MjM3Mjc2MCwibmJmIjoxNzcxNzY3OTYwLCJpYXQiOjE3NzE3Njc5NjAsImp0aSI6IjAxOWM4NTk5LTk1OTQtN2I3Ni05YjFiLTM5ZDIwYjdiYjBkOCIsInJvbGUiOiJSRUZSRVNIIn0.PoIUBr5u8BtGX5UjOZ0UDKSXF1A3OTtlEr2D2b8DB4s"
        ) @NotNull String token
) {

}
