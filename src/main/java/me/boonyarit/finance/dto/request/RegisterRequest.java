package me.boonyarit.finance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for user registration")
public record RegisterRequest(
    @NotBlank(message = "First name is required")
    @Schema(
        description = "User's first name",
        example = "John",
        type = "string"
    )
    String firstName,

    @NotBlank(message = "Last name is required")
    @Schema(
        description = "User's last name",
        example = "Doe",
        type = "string"
    )
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(
        description = "User's email address",
        example = "john.doe@example.com",
        type = "string"
    )
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(
        description = "User's password (minimum 8 characters)",
        example = "SecurePassword123",
        type = "string",
        format = "password",
        minLength = 8
    )
    String password
) {
}
