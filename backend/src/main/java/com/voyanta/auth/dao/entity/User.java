package com.voyanta.auth.dao.entity;

import com.voyanta.auth.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    // MÜVƏQQƏTİ İSTİFADƏ OLUNMUR — qeydiyyat yalnız Google ilədir. DB-də `password_hash`
    // sütunu qalır (silinməyib), sadəcə Java tərəfi artıq buna yazmır/oxumur. Hibernate
    // `ddl-auto: validate` DB-də əlavə/mapping olunmamış sütuna görə xəta vermir, ona görə
    // yeni migration lazım deyil.
    // private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}