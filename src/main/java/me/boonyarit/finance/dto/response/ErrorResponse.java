package me.boonyarit.finance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    Map<String, String> validationErrors
) {

    public ErrorResponse(HttpStatus status, String message) {
        this(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, null);
    }

    public ErrorResponse(HttpStatus status, String message, Map<String, String> fieldErrors) {
        this(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
