package com.voyanta.survey.dto.shared;

import com.voyanta.survey.enums.DurationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TravelDatesTest {

    @Test
    void acceptsExactDatesWithoutApproximateDuration() {
        assertThatCode(() -> new TravelDates(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), null))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsApproximateDurationWithoutExactDates() {
        assertThatCode(() -> new TravelDates(null, null, DurationType.DAYS_5_7))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBothOrNeither() {
        assertThatThrownBy(() -> new TravelDates(null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TravelDates(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), DurationType.WEEKEND_2_DAYS))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
