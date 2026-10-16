-- Flyway migration: V6__favorites.sql
-- Generic favorites for destinations, plans, places, etc.

CREATE SCHEMA IF NOT EXISTS favorite;

CREATE TABLE favorite.favorites (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    item_type   VARCHAR(32) NOT NULL,
    item_id     VARCHAR(128) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, item_type, item_id)
);

CREATE INDEX idx_favorites_user_id ON favorite.favorites(user_id);
