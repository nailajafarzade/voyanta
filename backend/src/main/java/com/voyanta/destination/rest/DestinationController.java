package com.voyanta.destination.rest;

import com.voyanta.common.dto.ApiResponse;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.destination.service.DestinationService;
import com.voyanta.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller that exposes the /api/destinations endpoints.
 *
 * <p>Public discovery lists:
 * <ul>
 *   <li>{@code /featured}  — editorially selected (unchanged behaviour)</li>
 *   <li>{@code /trending}  — recently saved destinations</li>
 *   <li>{@code /popular}   — most saved destinations</li>
 * </ul>
 *
 * <p><b>Why trending/popular live here and not under /api/recommendations:</b>
 * this whole path is already in {@code SecurityConfig.PUBLIC_ENDPOINTS}, so guests
 * receive them without any change to the security rules. The personalized
 * "Selected For You" list stays behind authentication on /api/recommendations,
 * because it is derived from one specific user's behaviour.
 *
 * <p>The optional {@code exclude} parameter takes destination ids that another
 * section on the same page already shows. It reduces visual repetition, but never
 * at the cost of relevance — if there is nothing else to return, the server tops
 * the list up rather than showing a shorter section.
 */

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;
    private final RecommendationService recommendationService;

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> featured(
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.getFeatured(limit)));
    }

    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> batch(@RequestParam List<UUID> ids) {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.getByIds(ids)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> bySeason(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Integer limit
    ) {
        if (season == null || season.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.ok(destinationService.getBySeason(season, limit)));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> trending(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) List<UUID> exclude
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                recommendationService.getTrending(limit, toSet(exclude))));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> popular(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) List<UUID> exclude
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                recommendationService.getPopular(limit, toSet(exclude))));
    }

    private java.util.Set<UUID> toSet(List<UUID> exclude) {
        return exclude == null ? java.util.Set.of() : new java.util.LinkedHashSet<>(exclude);
    }
}
