package me.boonyarit.finance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for user authentication")
public record AuthenticationRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(
        description = "User's email address",
        example = "user@example.com",
        type = "string"
    )
    String email,

    @NotBlank(message = "Password is required")
    @Schema(
        description = "User's password",
        example = "SecurePassword123",
        type = "string",
        format = "password"
    )
    String password
) {
}
