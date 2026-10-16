package com.voyanta.plan.dao.entity;


import com.voyanta.plan.dto.shared.BudgetSummary;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.InterestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JPA Entity mapped to the "plan.travel_plans" table. It stores one generated travel
 * plan: the chosen destination, dates, companion, interests, budget summary, generation
 * status and its itinerary days. The user id stays null while the plan is anonymous.
 */
@Entity
@Table(name = "travel_plans", schema = "plan")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlan {

    @Id
    @GeneratedValue
    private UUID id;

    // Qeydiyyatsız plan üçün null. Diqqət: auth.entity.User heç vaxt import olunmur,
    // yalnız id saxlanılır — modullar arası sərhəd bunun üçündür.
    private UUID userId;

    // Anonim yaradılan planı sonradan userId-yə bağlamaq (claim) və idempotency üçün
    @Column(unique = true)
    private String anonymousSessionId;

    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private CompanionType companion;

    // Recommendations (BE-2) üçün — survey-dən creation zamanı köçürülür,
    // istifadəçinin keçmiş planları üzrə aqreqasiya edilib maraq profili qurulur.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Set<InterestType> interests;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private BudgetSummary budgetSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<ItineraryDay> days = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}