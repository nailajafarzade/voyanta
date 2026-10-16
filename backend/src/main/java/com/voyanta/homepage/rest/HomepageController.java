package com.voyanta.homepage.rest;

import com.voyanta.common.dto.ApiResponse;
import com.voyanta.homepage.dto.response.HomepageStatsResponse;
import com.voyanta.homepage.service.HomepageStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller that exposes the /api/homepage endpoints. It serves the public "stats"
 * numbers shown on the landing page and delegates the work to HomepageStatsService.
 */
@RestController
@RequestMapping("/api/homepage")
@RequiredArgsConstructor
public class HomepageController {

    private final HomepageStatsService homepageStatsService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<HomepageStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(homepageStatsService.getStats()));
    }
}