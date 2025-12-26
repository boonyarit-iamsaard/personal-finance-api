package me.boonyarit.finance.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "security.jwt")
@RequiredArgsConstructor
public class RefreshTokenProperties {

    private final long refreshExpirationMs;

    private final long repeatIntervalMs;
}
