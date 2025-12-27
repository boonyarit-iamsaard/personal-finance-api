package me.boonyarit.finance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Response object containing user information")
public record UserResponse(
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
        description = "User's email address",
        example = "john.doe@example.com"
    )
    String email,

    @Schema(
        description = "Authentication provider used (e.g., LOCAL, GOOGLE)",
        example = "LOCAL"
    )
    String provider,

    @Schema(
        description = "User's role in the system",
        example = "USER"
    )
    String role
) {
}
