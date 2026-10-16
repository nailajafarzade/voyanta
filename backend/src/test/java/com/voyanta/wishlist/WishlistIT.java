package com.voyanta.wishlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistIT extends AbstractIntegrationTest {

    @Test
    void wishlistRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/wishlist")).andExpect(status().isForbidden());
    }

    @Test
    void addListAndRemoveWishlistItems() throws Exception {
        JsonNode auth = loginGoogle("wish-" + System.nanoTime() + "@voyanta.test", "Wisher");
        String token = auth.path("accessToken").asText();
        JsonNode featured = json(mockMvc.perform(get("/api/destinations/featured"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        String destinationId = featured.get(0).path("id").asText();

        mockMvc.perform(withAuth(post("/api/wishlist/" + destinationId), token))
                .andExpect(status().isOk());
        mockMvc.perform(withAuth(post("/api/wishlist/" + destinationId), token))
                .andExpect(status().isOk());

        JsonNode list = json(mockMvc.perform(withAuth(get("/api/wishlist"), token))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).path("id").asText()).isEqualTo(destinationId);

        mockMvc.perform(withAuth(delete("/api/wishlist/" + destinationId), token))
                .andExpect(status().isOk());
        JsonNode empty = json(mockMvc.perform(withAuth(get("/api/wishlist"), token))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(empty.size()).isZero();
    }

    @Test
    void addingUnknownDestinationReturnsNotFound() throws Exception {
        JsonNode auth = loginGoogle("wish-missing-" + System.nanoTime() + "@voyanta.test", "Wisher");
        mockMvc.perform(withAuth(post("/api/wishlist/" + UUID.randomUUID()), auth.path("accessToken").asText()))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("RESOURCE_NOT_FOUND"));
    }
}
