package com.voyanta.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.BudgetTier;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.HotelType;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.survey.enums.MealPreference;
import com.voyanta.survey.enums.TripPurpose;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder(new ObjectMapper());

    @Test
    void userMessageIncludesSurveyFields() {
        AiRequest request = new AiRequest(
                Set.of(InterestType.SEA, InterestType.NATURE),
                CompanionType.FAMILY,
                new FamilyDetails(2, 1),
                new Budget(BudgetTier.MID_RANGE, null),
                new TravelDates(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), null),
                HotelType.FOUR_STAR,
                MealPreference.BREAKFAST_INCLUDED,
                Set.of(TripPurpose.RELAXATION)
        );

        String message = promptBuilder.buildUserMessage(request);
        assertThat(message).contains("SEA").contains("NATURE");
        assertThat(message).contains("FAMILY").contains("2 böyük").contains("1 uşaq");
        assertThat(message).contains("MID_RANGE").contains("2026-10-01");
        assertThat(message).contains("FOUR_STAR").contains("BREAKFAST_INCLUDED").contains("RELAXATION");
    }

    /**
     * Regression test for "the planner ignores my answers": the user message is the
     * ONLY place the survey answers reach the model, so two different answer
     * combinations must yield two different messages containing their own values.
     */
    @Test
    void differentAnswerCombinationsProduceDifferentUserMessages() {
        AiRequest seaFamily = new AiRequest(
                Set.of(InterestType.SEA),
                CompanionType.FAMILY,
                new FamilyDetails(2, 1),
                new Budget(BudgetTier.PREMIUM, null),
                new TravelDates(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), null),
                HotelType.FIVE_STAR,
                MealPreference.ALL_INCLUSIVE,
                Set.of(TripPurpose.HONEYMOON)
        );
        AiRequest mountainsSolo = new AiRequest(
                Set.of(InterestType.NATURE),
                CompanionType.SOLO,
                null,
                new Budget(BudgetTier.ECONOMY, null),
                new TravelDates(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), null),
                HotelType.THREE_STAR,
                MealPreference.NO_MEALS,
                Set.of(TripPurpose.ADVENTURE)
        );

        String first = promptBuilder.buildUserMessage(seaFamily);
        String second = promptBuilder.buildUserMessage(mountainsSolo);

        assertThat(first)
                .as("different answers must produce a different user message")
                .isNotEqualTo(second)
                .contains("SEA", "FAMILY", "PREMIUM", "FIVE_STAR", "ALL_INCLUSIVE", "HONEYMOON");
        assertThat(second)
                .contains("NATURE", "SOLO", "ECONOMY", "THREE_STAR", "NO_MEALS", "ADVENTURE");
    }

    /** The frontend always sends a tier (CUSTOM + amount), so the amount must not be lost. */
    @Test
    void customBudgetAmountIsPartOfTheUserMessage() {
        AiRequest request = new AiRequest(
                Set.of(InterestType.SEA),
                CompanionType.SOLO,
                null,
                new Budget(BudgetTier.CUSTOM, 750),
                new TravelDates(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), null),
                HotelType.BOUTIQUE,
                MealPreference.BREAKFAST_INCLUDED,
                Set.of(TripPurpose.RELAXATION)
        );

        assertThat(promptBuilder.buildUserMessage(request))
                .contains("CUSTOM")
                .contains("750");
    }

    /**
     * Real destination names in the system prompt anchored the model: it kept
     * returning those same places regardless of the answers. Examples, if needed,
     * must be neutral.
     */
    @Test
    void systemPromptContainsNoConcreteDestinationExamples() {
        assertThat(PromptBuilder.SYSTEM_PROMPT)
                .doesNotContainIgnoringCase(
                        "maldives", "maldiv", "santorini", "florence", "florensiya",
                        "florida", "rome", "bali", "paris");
    }

    @Test
    void outputSchemaIsValidJsonObject() {
        Map<String, Object> schema = promptBuilder.outputSchema();
        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema).containsKey("properties");
        assertThat(schema).containsKey("required");
    }
}
