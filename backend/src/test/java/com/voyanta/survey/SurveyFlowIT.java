package com.voyanta.survey;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SurveyFlowIT extends AbstractIntegrationTest {

    @Test
    void createGetAndPatchSurveySessionInRedis() throws Exception {
        JsonNode created = createSurveySession();
        String sessionId = created.path("sessionId").asText();
        assertThat(sessionId).isNotBlank();
        assertThat(created.path("completed").asBoolean()).isFalse();

        JsonNode fetched = json(mockMvc.perform(get("/api/survey/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(fetched.path("sessionId").asText()).isEqualTo(sessionId);

        completeSurvey(sessionId);

        JsonNode updated = json(mockMvc.perform(get("/api/survey/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(updated.path("companion").asText()).isEqualTo("COUPLE");
        assertThat(updated.path("hotelType").asText()).isEqualTo("FOUR_STAR");
        assertThat(updated.path("dates").path("startDate").asText()).isEqualTo("2026-10-01");
        assertThat(updated.path("interests").toString()).contains("SEA");
    }

    @Test
    void patchMergesOnlyProvidedFields() throws Exception {
        String sessionId = createSurveySession().path("sessionId").asText();
        mockMvc.perform(patch("/api/survey/sessions/" + sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companion\":\"SOLO\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/survey/sessions/" + sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelType\":\"VILLA\"}"))
                .andExpect(status().isOk());

        JsonNode session = json(mockMvc.perform(get("/api/survey/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(session.path("companion").asText()).isEqualTo("SOLO");
        assertThat(session.path("hotelType").asText()).isEqualTo("VILLA");
    }

    @Test
    void missingSessionReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/survey/sessions/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("RESOURCE_NOT_FOUND"));
    }
}
