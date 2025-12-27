package me.boonyarit.finance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for user logout")
public record LogoutRequest(
    @NotBlank(message = "Refresh token is required")
    @Schema(
        description = "Refresh token to invalidate",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        type = "string"
    )
    String refreshToken
) {
}
