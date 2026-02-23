package fr.anisekai.web.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class WebException extends RuntimeException {


    @Schema(name = "WebException", description = "Used when something isn't going how it is supposed to go.")
    public record Dto(
            @Schema(
                    description = "Time at which the error has been generated.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            ) @NotNull ZonedDateTime timestamp,

            @Schema(
                    description = "Status code for the error.",
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    minimum = "100",
                    maximum = "511"
            ) int status,

            @Schema(
                    description = "Friendly message for the error.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            ) @NotNull String message
    ) {

        public static Dto from(WebException ex, boolean details) {

            return new Dto(ex.timestamp, ex.status.value(), ex.userMessage);
        }

    }

    public final String        message;
    public final HttpStatus    status;
    public final String        userMessage;
    public final String        reason;
    public final ZonedDateTime timestamp;

    public WebException(HttpStatus status, String message) {

        super(message);
        this.message     = message;
        this.status      = status;
        this.userMessage = null;
        this.reason      = null;
        this.timestamp   = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public WebException(HttpStatus status, String message, String userMessage) {

        super(message);
        this.message     = message;
        this.status      = status;
        this.userMessage = userMessage;
        this.reason      = null;
        this.timestamp   = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public WebException(HttpStatus status, String message, String userMessage, Throwable cause) {

        super(message, cause);
        this.message     = message;
        this.status      = status;
        this.userMessage = userMessage;
        this.reason      = cause.getMessage();
        this.timestamp   = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public WebException(HttpStatus status, String message, Throwable cause) {

        super(message, cause);
        this.message     = message;
        this.status      = status;
        this.userMessage = null;
        this.reason      = cause.getMessage();
        this.timestamp   = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

}
