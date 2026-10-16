package com.voyanta.survey.dto.shared;

import com.voyanta.survey.enums.DurationType;

import java.time.LocalDate;

/**
 * Shared DTO for the travel timing answer. It holds either exact start and end dates or
 * an approximate duration, and its constructor rejects any input that sets both or
 * neither.
 */
public record TravelDates(
        LocalDate startDate,
        LocalDate endDate,
        DurationType approximateDuration
) {
    public TravelDates {
        boolean hasExactDates = startDate != null && endDate != null;
        boolean hasApproxDuration = approximateDuration != null;

        if (hasExactDates == hasApproxDuration) {
            throw new IllegalArgumentException(
                    "Ya dəqiq tarixlər, ya da təxmini müddət seçilməlidir — ikisi eyni vaxtda ola bilməz"
            );
        }
    }
}