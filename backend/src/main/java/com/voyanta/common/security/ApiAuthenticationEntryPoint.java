package com.voyanta.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Answers requests that reach a protected endpoint without a valid login.
 *
 * Spring Security's built-in entry point writes an empty body, so the client had no
 * machine-readable reason for the rejection - only a status code. This one writes the
 * same ApiResponse envelope every other Voyanta endpoint uses, with the AUTH_REQUIRED
 * code the plan endpoints already use for "you need to sign in".
 *
 * Registered in SecurityConfig via exceptionHandling().authenticationEntryPoint(...).
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * The status stays 403, exactly as Spring Security's default
     * Http403ForbiddenEntryPoint produced it before. Changing it to 401 would alter the
     * contract every existing protected endpoint (and its frontend handling) relies on,
     * which is outside the scope of this feature. Only the body changes: empty -> envelope.
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        // A generic message only: the underlying exception can carry internal details.
        write(response, HttpStatus.FORBIDDEN, "AUTH_REQUIRED", "Bu məlumatı görmək üçün daxil olmalısan");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}
