package com.voyanta.destination;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DestinationIT extends AbstractIntegrationTest {

    @Test
    void featuredDestinationsComeFromTheDatabase() throws Exception {
        JsonNode data = json(mockMvc.perform(get("/api/destinations/featured"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(data.isArray()).isTrue();
        // The catalogue has grown beyond the original four seeded rows — the contract
        // is "featured comes from the database", not a fixed count.
        assertThat(data.size()).isGreaterThanOrEqualTo(4);
        assertThat(data.get(0).path("name").asText()).isNotBlank();
        assertThat(data.get(0).path("country").asText()).isNotBlank();
        assertThat(data.get(0).path("imageUrl").asText()).isNotBlank();
    }

    @Test
    void featuredLimitIsHonored() throws Exception {
        JsonNode data = json(mockMvc.perform(get("/api/destinations/featured").param("limit", "2"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(data.size()).isEqualTo(2);
    }
}
