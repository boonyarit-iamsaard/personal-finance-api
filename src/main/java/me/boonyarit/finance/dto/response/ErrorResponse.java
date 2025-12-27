package me.boonyarit.finance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response structure")
public record ErrorResponse(
    @Schema(
        description = "Timestamp when the error occurred",
        example = "2024-06-15T14:30:00"
    )
    LocalDateTime timestamp,

    @Schema(
        description = "HTTP status code",
        example = "400"
    )
    int status,

    @Schema(
        description = "Error reason phrase",
        example = "Bad Request"
    )
    String error,

    @Schema(
        description = "Detailed error message",
        example = "Validation failed for object='user'. Error count: 1"
    )
    String message,

    @Schema(
        description = "Field-specific validation errors",
        example = "{ \"email\": \"must be a well-formed email address\" }"
    )
    Map<String, String> validationErrors
) {

    public ErrorResponse(HttpStatus status, String message) {
        this(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, null);
    }

    public ErrorResponse(HttpStatus status, String message, Map<String, String> fieldErrors) {
        this(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
