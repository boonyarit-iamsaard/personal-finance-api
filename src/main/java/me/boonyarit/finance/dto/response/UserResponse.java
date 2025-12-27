package me.boonyarit.finance.dto.response;

import lombok.Builder;

@Builder
public record UserResponse(
    String firstName,
    String lastName,
    String email,
    String provider,
    String role
) {
}
