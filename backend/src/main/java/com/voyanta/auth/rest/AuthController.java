package com.voyanta.auth.rest;


import com.voyanta.auth.dto.request.OAuthLoginRequest;
import com.voyanta.auth.dto.request.RefreshRequest;
import com.voyanta.auth.dto.response.AuthResponse;
import com.voyanta.auth.service.AuthService;
import com.voyanta.auth.service.OAuthService;
import com.voyanta.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;

    /*
     * MÜVƏQQƏTİ SÖNDÜRÜLÜB — qeydiyyat/giriş hazırda yalnız Google ilədir.
     * Email/şifrə qaytarılsa, bu iki endpoint-i açmaq kifayətdir (AuthService.register/login
     * hələ mövcuddur, onlar da kommentdədir — bax AuthService.java).
     *
     * @PostMapping("/register")
     * public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
     *     return ResponseEntity.ok(ApiResponse.ok(authService.register(request)));
     * }
     *
     * @PostMapping("/login")
     * public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
     *     return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
     * }
     */

    @PostMapping("/oauth/{provider}")
    public ResponseEntity<ApiResponse<AuthResponse>> oauth(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(oAuthService.login(provider, request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UUID userId) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}