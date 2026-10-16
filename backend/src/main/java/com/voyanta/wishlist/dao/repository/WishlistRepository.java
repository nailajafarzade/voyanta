package com.voyanta.wishlist.dao.repository;

import com.voyanta.wishlist.dao.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<WishlistItem, UUID> {
    boolean existsByUserIdAndDestinationId(UUID userId, UUID destinationId);
    List<WishlistItem> findByUserId(UUID userId);
    void deleteByUserIdAndDestinationId(UUID userId, UUID destinationId);

    /**
     * REAL popularity signal: how many users have each destination in their wishlist.
     *
     * <p>Destinations nobody favourited are absent from the result (not zero-filled),
     * so a caller can tell "no data" apart from "zero favourites".
     */
    @Query("select w.destinationId as destinationId, count(w) as favorites "
            + "from WishlistItem w group by w.destinationId")
    List<DestinationFavoriteCount> countFavoritesPerDestination();

    /**
     * REAL trend signal: favourites added since a point in time.
     *
     * <p>Uses {@code created_at}, which is set when the user actually saved the
     * destination. Combined with {@link #countFavoritesPerDestination()} this gives
     * "what is popular right now" instead of "what was always popular".
     */
    @Query("select w.destinationId as destinationId, count(w) as favorites "
            + "from WishlistItem w where w.createdAt >= :since group by w.destinationId")
    List<DestinationFavoriteCount> countFavoritesPerDestinationSince(@Param("since") Instant since);
}