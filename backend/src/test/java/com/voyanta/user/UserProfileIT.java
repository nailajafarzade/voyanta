package com.voyanta.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileIT extends AbstractIntegrationTest {

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedInUserCanReadAndUpdateOwnProfile() throws Exception {
        String email = "profile-" + System.nanoTime() + "@voyanta.test";
        JsonNode auth = loginGoogle(email, "Original Name");
        String token = auth.path("accessToken").asText();

        JsonNode profile = json(mockMvc.perform(withAuth(get("/api/users/me"), token))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(profile.path("email").asText()).isEqualTo(email);
        assertThat(profile.path("fullName").asText()).isEqualTo("Original Name");

        JsonNode updated = json(mockMvc.perform(withAuth(put("/api/users/me"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Name\",\"phone\":\"+994501112233\"}"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(updated.path("fullName").asText()).isEqualTo("Updated Name");
        assertThat(updated.path("phone").asText()).isEqualTo("+994501112233");
        assertThat(updated.path("email").asText()).isEqualTo(email);
    }

    @Test
    void blankFullNameFailsValidation() throws Exception {
        JsonNode auth = loginGoogle("blank-" + System.nanoTime() + "@voyanta.test", "Name");
        mockMvc.perform(withAuth(put("/api/users/me"), auth.path("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("VALIDATION_FAILED"));
    }
}
