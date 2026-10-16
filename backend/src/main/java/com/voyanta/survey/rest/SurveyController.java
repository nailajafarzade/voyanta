package com.voyanta.survey.controller;

import com.voyanta.common.dto.ApiResponse;
import com.voyanta.survey.dto.request.UpdateSurveyRequest;
import com.voyanta.survey.dto.response.SurveySession;
import com.voyanta.survey.service.SurveySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/survey/sessions")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveySessionService surveySessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SurveySession>> create() {
        return ResponseEntity.ok(ApiResponse.ok(surveySessionService.create()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SurveySession>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(surveySessionService.get(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SurveySession>> update(
            @PathVariable String id,
            @RequestBody UpdateSurveyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(surveySessionService.update(id, request)));
    }
}