package com.voyanta.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GlobalExceptionHandler.
 *
 * Two things matter here:
 *  1. The HTTP contract must not change. Clients (and the existing frontend) branch
 *     on status + errorCode, so a fix for logging must not alter any response.
 *  2. Unexpected exceptions must actually be logged. The original handler returned
 *     a 500 with no logging at all, which is precisely why the real cause of the
 *     production 401 could never be diagnosed from the logs.
 */
@ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void unexpectedExceptionIsLoggedWithItsCause(org.springframework.boot.test.system.CapturedOutput output) {
        IllegalStateException cause = new IllegalStateException("provider timeout after 3 attempts");

        handler.handleUnexpected(cause);

        assertThat(output)
                .as("an unexpected exception must leave a diagnosable trace in the log")
                .contains("provider timeout after 3 attempts")
                .contains(IllegalStateException.class.getName());
    }

    @Test
    void unexpectedExceptionStillReturnsGenericFiveHundred() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnexpected(new RuntimeException("secret-token-abc123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getError())
                .as("the raw exception message must not be echoed to the client")
                .doesNotContain("secret-token-abc123");
    }

    @Test
    void apiExceptionStatusAndCodeArePassedThroughUnchanged() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(
                new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_TOKEN", "OAuth token doğrulanmadı"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_OAUTH_TOKEN");
        assertThat(response.getBody().getError()).isEqualTo("OAuth token doğrulanmadı");
    }

    @Test
    void serviceUnavailableApiExceptionKeepsItsStatus() {
        // 503 OAUTH_PROVIDER_UNAVAILABLE is the new contract for provider outages.
        // It must survive the handler untouched, otherwise the fix would be invisible
        // to the client.
        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(
                new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OAUTH_PROVIDER_UNAVAILABLE", "hazır deyil"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("OAUTH_PROVIDER_UNAVAILABLE");
    }

    @Test
    void serverErrorsFromApiExceptionsAreLogged(org.springframework.boot.test.system.CapturedOutput output) {
        handler.handleApiException(
                new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE", "rate limited"));

        assertThat(output)
                .as("5xx ApiExceptions are deliberate and should be visible without a stack trace")
                .contains("AI_UNAVAILABLE");
    }

    @Test
    void clientErrorsFromApiExceptionsAreNotLoggedAsErrors(
            org.springframework.boot.test.system.CapturedOutput output) {
        handler.handleApiException(
                new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "bad input"));

        assertThat(output)
                .as("expected 4xx responses are not incidents and should not spam the log")
                .doesNotContain("VALIDATION_FAILED");
    }

    @Test
    void resourceNotFoundKeepsItsOwnContract() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleApiException(new ResourceNotFoundException("Plan tapılmadı"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void rateLimitExceededKeepsIts429Contract() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleApiException(new RateLimitExceededException("Gündəlik plan limitinə çatmısan"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void handlerIsRegisteredAsRestControllerAdvice() {
        // Guards against someone moving the class out of the @RestControllerAdvice
        // component scan, which would silently disable every mapping above.
        assertThat(GlobalExceptionHandler.class.getAnnotation(org.springframework.web.bind.annotation.RestControllerAdvice.class))
                .isNotNull();
        assertThat(LoggerFactory.getLogger(GlobalExceptionHandler.class)).isNotNull();
    }

    @Test
    void successfulResponsesAreNotAffected() {
        // Sanity check that the envelope helper the handler relies on is intact.
        ApiResponse<String> ok = ApiResponse.ok("value");
        assertThat(ok.isSuccess()).isTrue();
        assertThat(ok.getData()).isEqualTo("value");

        Map<String, String> errors = Map.of("field", "message");
        assertThat(errors).containsEntry("field", "message");
    }
}
