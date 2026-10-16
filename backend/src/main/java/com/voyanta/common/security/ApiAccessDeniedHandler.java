package com.voyanta.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Answers requests that pass the URL rules but are refused by method security
 * (@PreAuthorize), for example an anonymous call to a controller method that requires
 * an authenticated user.
 *
 * Before method security existed, an anonymous request never reached a controller, so
 * this path was unreachable and the response had no body. Now that @PreAuthorize is
 * used on personalized endpoints, an AccessDeniedException can surface here, and the
 * generic Exception handler in GlobalExceptionHandler would otherwise turn it into a
 * 500. This handler keeps it a clean 403 with the standard envelope.
 */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        // Spring's AnonymousAuthenticationToken is the tell-tale of a guest: it means the
        // caller never signed in, so the message should point at signing in.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;

        if (anonymous) {
            write(response, "AUTH_REQUIRED", "Bu məlumatı görmək üçün daxil olmalısan");
        } else {
            write(response, "FORBIDDEN", "Bu əməliyyat üçün icazən yoxdur");
        }
    }

    private void write(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}
