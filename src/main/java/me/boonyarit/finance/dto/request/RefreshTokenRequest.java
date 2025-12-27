package me.boonyarit.finance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for refreshing authentication token")
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    @Schema(
        description = "Refresh token issued during authentication or registration",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        type = "string"
    )
    String refreshToken
) {
}
