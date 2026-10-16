package com.voyanta.auth.dao.repository;

import com.voyanta.auth.dao.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the RefreshToken entity. It finds an active (not revoked)
 * token by its hash when refreshing an access token, and deletes all tokens of a user
 * on logout.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
    void deleteByUserId(UUID userId);
}