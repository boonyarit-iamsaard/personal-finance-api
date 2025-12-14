package me.boonyarit.finance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.boonyarit.finance.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
