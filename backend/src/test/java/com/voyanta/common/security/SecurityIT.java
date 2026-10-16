package com.voyanta.common.security;

import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends AbstractIntegrationTest {

    @Test
    void publicEndpointsAreReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/destinations/featured")).andExpect(status().isOk());
        mockMvc.perform(get("/api/homepage/stats")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsRejectAnonymousAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/wishlist")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/recommendations/personalized")).andExpect(status().isForbidden());
    }

    @Test
    void corsPreflightFromAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/destinations/featured")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
