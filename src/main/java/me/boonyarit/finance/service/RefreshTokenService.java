package me.boonyarit.finance.service;

import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.entity.RefreshTokenEntity;
import me.boonyarit.finance.entity.UserEntity;
import me.boonyarit.finance.exception.RefreshTokenException;
import me.boonyarit.finance.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${security.jwt.refresh-expiration-ms}")
    private Long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenEntity createRefreshToken(UserEntity user) {
        refreshTokenRepository.revokeAllValidTokensByUser(user);

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(now().plus(refreshTokenExpirationMs, MILLIS))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshTokenEntity findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElseThrow(() -> new RefreshTokenException("Refresh token not found"));
    }

    public RefreshTokenEntity verifyExpiration(RefreshTokenEntity token) {
        if (token.getExpiryDate().isBefore(now()) || token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException("Refresh token has expired or revoked. Please login again.");
        }

        return token;
    }

    @Transactional
    public RefreshTokenEntity refreshToken(String requestRefreshToken) {
        RefreshTokenEntity token = findByToken(requestRefreshToken);
        RefreshTokenEntity verifiedToken = verifyExpiration(token);

        verifiedToken.setRevoked(true);
        refreshTokenRepository.save(verifiedToken);

        return createRefreshToken(verifiedToken.getUser());
    }
}
