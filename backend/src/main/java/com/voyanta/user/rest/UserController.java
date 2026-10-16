package com.voyanta.user.rest;

import com.voyanta.common.dto.ApiResponse;

import com.voyanta.user.dto.request.UpdateProfileRequest;
import com.voyanta.user.dto.response.UserProfileResponse;
import com.voyanta.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller that exposes the /api/users endpoints for the signed-in user's own
 * profile: reading it and updating it. The user id always comes from the security
 * context, so nobody can read or change another account.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userProfileService.getProfile(userId)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userProfileService.updateProfile(userId, request)));
    }
}