package com.voyanta.auth.service;

import com.voyanta.auth.dao.entity.RefreshToken;
import com.voyanta.auth.dao.entity.User;
import com.voyanta.auth.dao.repository.RefreshTokenRepository;
import com.voyanta.auth.dao.repository.UserRepository;
import com.voyanta.auth.dto.request.RefreshRequest;
import com.voyanta.auth.dto.response.AuthResponse;
import com.voyanta.auth.dto.response.UserSummary;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ApiException;
import com.voyanta.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    // MÜVƏQQƏTİ İSTİFADƏ OLUNMUR — yalnız register()/login() bunu çağırırdı, ikisi də kommentdədir.
    // Email/şifrə qaytarılsa, aşağını da geri aç.
    // private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VoyantaProperties properties;

    /*
     * MÜVƏQQƏTİ SÖNDÜRÜLÜB — qeydiyyat hazırda yalnız Google ilədir (AuthController-də
     * /register və /login endpoint-ləri də kommentdədir). Geri açanda passwordEncoder
     * field-ini də yuxarıda geri aç.
     *
     * @Transactional
     * public AuthResponse register(RegisterRequest request) {
     *     if (userRepository.existsByEmail(request.email())) {
     *         throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Bu e-poçt artıq qeydiyyatdan keçib");
     *     }
     *
     *     User user = User.builder()
     *             .fullName(request.fullName())
     *             .email(request.email())
     *             .phone(request.phone())
     *             .passwordHash(passwordEncoder.encode(request.password()))
     *             .provider(AuthProvider.LOCAL)
     *             .build();
     *
     *     userRepository.save(user);
     *     return issueTokens(user);
     * }
     *
     * @Transactional
     * public AuthResponse login(LoginRequest request) {
     *     User user = userRepository.findByEmail(request.email())
     *             .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-poçt və ya şifrə yanlışdır"));
     *
     *     if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
     *         throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-poçt və ya şifrə yanlışdır");
     *     }
     *
     *     return issueTokens(user);
     * }
     */

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = hash(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token etibarsızdır"));

        if (stored.getExpiresAt().isBefore(Instant.now()) || !jwtService.isTokenValid(request.refreshToken())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh token vaxtı bitib, yenidən daxil ol");
        }

        // Rotasiya: bu refresh token bir daha işləməyəcək, hətta sızsa belə təkrar istifadə mümkün olmasın
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "İstifadəçi tapılmadı"));

        return issueTokens(user);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    // package-private: OAuthService (eyni paketdə) də bunu çağırır, token
    // yaratma/hash-ləmə məntiqini iki yerdə təkrarlamamaq üçün.
    AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(refreshToken))
                .expiresAt(Instant.now().plus(properties.getJwt().getRefreshTokenTtl()))
                .build();

        refreshTokenRepository.save(entity);

        return new AuthResponse(
                accessToken,
                refreshToken,
                new UserSummary(user.getId(), user.getFullName(), user.getEmail())
        );
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 mövcud deyil", e);
        }
    }
}