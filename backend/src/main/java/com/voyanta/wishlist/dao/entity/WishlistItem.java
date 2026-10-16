package com.voyanta.wishlist.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wishlist_items",
        schema = "wishlist",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "destination_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItem {

    @Id
    @GeneratedValue
    private UUID id;

    // Diqqət: auth.entity.User və destination.entity.Destination heç biri import olunmur —
    // yalnız UUID-lər saxlanılır, TravelPlan-da olduğu kimi eyni prinsip.
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "destination_id", nullable = false)
    private UUID destinationId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}