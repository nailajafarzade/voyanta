package com.voyanta.plan.dao.entity;

import com.voyanta.plan.dto.shared.ItineraryItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * JPA Entity mapped to the "plan.itinerary_days" table. It holds one day of a travel
 * plan: its day number and the activities stored as JSON, plus a locked flag that hides
 * the activities from users who have not signed in.
 */
@Entity
@Table(name = "itinerary_days", schema = "plan")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDay {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TravelPlan plan;

    private int dayNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<ItineraryItem> items;

    @Builder.Default
    private boolean locked = false;
}
//hi