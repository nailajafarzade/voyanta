package com.voyanta.auth.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity mapped to the "auth.refresh_tokens" table. Each row is one issued refresh
 * token for a user, kept only as a SHA-256 hash together with its expiry time and a
 * revoked flag, so used or rotated tokens can no longer be reused.
 */
@Entity
@Table(name = "refresh_tokens", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    // DB sızsa belə işə yaramasın deyə raw token yox, SHA-256 hash-i saxlanılır
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    private boolean revoked = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}