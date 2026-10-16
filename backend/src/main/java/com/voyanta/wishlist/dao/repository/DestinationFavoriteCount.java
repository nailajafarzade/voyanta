package com.voyanta.wishlist.dao.repository;

import java.util.UUID;

/**
 * Real, aggregated favourite data for one destination.
 *
 * <p>This is a Spring Data interface projection over
 * {@code wishlist.wishlist_items}. It counts rows that USERS actually created —
 * there is no seeded or synthetic data behind it. If a destination has zero
 * favourites, it simply does not appear in the result.
 *
 * @see WishlistRepository#countFavoritesPerDestination()
 */
public interface DestinationFavoriteCount {

    UUID getDestinationId();

    long getFavorites();
}