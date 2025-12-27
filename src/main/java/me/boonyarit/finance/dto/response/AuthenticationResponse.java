package me.boonyarit.finance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Response object containing authentication tokens and user information")
public record AuthenticationResponse(
    @Schema(
        description = "JWT access token (expires in 15 minutes)",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    String token,

    @Schema(
        description = "JWT refresh token (expires in 7 days)",
        example = "dGhpcy1pcz1hLXJlZnJlc2gtdG9rZW4uLi4="
    )
    String refreshToken,

    @Schema(
        description = "User's email address",
        example = "john.doe@example.com"
    )
    String email,

    @Schema(
        description = "User's first name",
        example = "John"
    )
    String firstName,

    @Schema(
        description = "User's last name",
        example = "Doe"
    )
    String lastName,

    @Schema(
        description = "Authentication provider used (e.g., LOCAL, GOOGLE)",
        example = "LOCAL"
    )
    String provider
) {
}
