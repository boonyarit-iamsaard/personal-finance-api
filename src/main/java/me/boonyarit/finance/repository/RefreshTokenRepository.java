package me.boonyarit.finance.repository;

import me.boonyarit.finance.entity.RefreshTokenEntity;
import me.boonyarit.finance.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false ")
    void revokeAllValidTokensByUser(@Param("user") UserEntity user);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiryDate < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);
}
