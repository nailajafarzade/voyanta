-- V3__wishlist.sql
-- Wishlist (destinasiya bookmark-ları) — həm plan nəticəsindəki 3 tövsiyə yerdən,
-- həm homepage suggestion kartlarından istifadə olunur; ikisi də eyni Destination-a işarə edir.

CREATE SCHEMA IF NOT EXISTS wishlist;

CREATE TABLE wishlist.wishlist_items (
                                         id             UUID PRIMARY KEY,
                                         user_id        UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
                                         destination_id UUID NOT NULL REFERENCES destination.destinations(id) ON DELETE CASCADE,
                                         created_at     TIMESTAMPTZ NOT NULL,
                                         UNIQUE (user_id, destination_id)
);

CREATE INDEX idx_wishlist_user_id ON wishlist.wishlist_items(user_id);