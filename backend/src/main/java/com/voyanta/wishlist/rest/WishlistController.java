package com.voyanta.wishlist.rest;

import com.voyanta.common.dto.ApiResponse;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> list(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.list(userId)));
    }

    @PostMapping("/{destinationId}")
    public ResponseEntity<ApiResponse<Void>> add(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID destinationId
    ) {
        wishlistService.add(userId, destinationId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{destinationId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID destinationId
    ) {
        wishlistService.remove(userId, destinationId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}