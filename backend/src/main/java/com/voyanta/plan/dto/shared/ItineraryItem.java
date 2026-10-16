package com.voyanta.plan.dto.shared;

// category bilərəkdən String-dir (enum yox) — AI-nin sərbəst kateqoriya adları
// qaytarmasına imkan verir, frontend rənglənməni buna görə edir
/**
 * Shared DTO for one activity in a day plan: its time, title, description and category.
 * It is saved as JSON in the ItineraryDay entity and sent back to the client for
 * unlocked days.
 */
public record ItineraryItem(
        String time,
        String title,
        String description,
        String category
) {}