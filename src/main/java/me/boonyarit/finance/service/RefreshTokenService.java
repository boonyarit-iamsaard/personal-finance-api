package me.boonyarit.finance.service;

import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.config.RefreshTokenProperties;
import me.boonyarit.finance.entity.RefreshTokenEntity;
import me.boonyarit.finance.entity.UserEntity;
import me.boonyarit.finance.exception.RefreshTokenException;
import me.boonyarit.finance.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;

    @Transactional
    public RefreshTokenEntity createRefreshToken(UserEntity user) {
        revokeAllValidTokensByUser(user);
        return generateAndSaveNewToken(user);
    }

    @Transactional
    public void revokeTokenByString(String token) {
        findByToken(token).ifPresent(this::revokeToken);
    }

    @Transactional
    public RefreshTokenEntity refreshToken(String tokenStr) {
        RefreshTokenEntity token = findByToken(tokenStr)
            .orElseThrow(() -> new RefreshTokenException(tokenStr, "Refresh token not found"));

        RefreshTokenEntity verifiedToken = verifyExpiration(token);

        if (verifiedToken.isRevoked()) {
            throw new RefreshTokenException(tokenStr, "Refresh token has been revoked");
        }

        UserEntity user = verifiedToken.getUser();
        revokeToken(verifiedToken);
        revokeAllValidTokensByUser(user);
        return generateAndSaveNewToken(user);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredTokens(now());
    }

    private Optional<RefreshTokenEntity> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    private RefreshTokenEntity verifyExpiration(RefreshTokenEntity token) {
        if (token.getExpiryDate().isBefore(now())) {
            throw new RefreshTokenException(token.getToken(), "Refresh token was expired. Please make a new login request");
        }
        return token;
    }

    private void revokeToken(RefreshTokenEntity token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private void revokeAllValidTokensByUser(UserEntity user) {
        refreshTokenRepository.revokeAllValidTokensByUser(user);
    }

    private RefreshTokenEntity generateAndSaveNewToken(UserEntity user) {
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
            .user(user)
            .token(UUID.randomUUID().toString())
            .expiryDate(now().plus(refreshTokenProperties.getRefreshExpirationMs(), MILLIS))
            .revoked(false)
            .build();

        return refreshTokenRepository.save(refreshToken);
    }
}
