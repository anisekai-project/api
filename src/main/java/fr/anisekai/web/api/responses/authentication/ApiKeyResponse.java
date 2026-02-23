package fr.anisekai.web.api.responses.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Structure of an api key response.")
public record ApiKeyResponse(
        @Schema(
                description = "A JWT value",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBbmlzZWthaSIsInN1YiI6IjE0OTI3OTE1MDY0ODA2NjA0OCIsImV4cCI6MTc3NDAzMTQxMywibmJmIjoxNzcxNDM5NDEzLCJpYXQiOjE3NzE0Mzk0MTMsImp0aSI6IjAxOWM3MjA0LTU4NWQtNzY0NS1hMmQ3LTY2MmI2NDMzOTVlMCIsInJvbGUiOiJBUFBMSUNBVElPTiJ9.DQKHJ8WQ3-Sk781lHgcZDflYgEhkmx_4Cq5F6MulP2I"
        ) @NotNull String key
) {

}
