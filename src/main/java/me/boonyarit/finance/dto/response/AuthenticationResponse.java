package me.boonyarit.finance.dto.response;

import lombok.Builder;

@Builder
public record AuthenticationResponse(
    String token,
    String email,
    String firstName,
    String lastName,
    String provider
) {
}
