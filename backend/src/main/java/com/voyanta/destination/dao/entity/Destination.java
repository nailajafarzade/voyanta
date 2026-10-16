package com.voyanta.destination.dao.entity;

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
import java.util.Set;
import java.util.UUID;

/**
 * JPA Entity mapped to the "destination.destinations" table. It stores one travel
 * destination that can be shown as a card (name, country, image, tag), plus the interest
 * tags used to match it to a user and a flag marking it as featured.
 */
@Entity
@Table(name = "destinations", schema = "destination")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Destination {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String imageUrl;

    // Kart üzərindəki göstərilən mətn (məs. "Tarix və mədəniyyət")
    @Column(nullable = false)
    private String tag;

    // Tövsiyə uyğunluğu hesablamaq üçün struktur data — "tag"-dan ayrıdır,
    // çünki tag sərbəst mətndir, bu isə InterestType-a qarşı hesablanandır.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Set<InterestType> interestTags;

    @Builder.Default
    private boolean featured = false;

    @Column
    private String season;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}