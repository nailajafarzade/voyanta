-- Flyway migration: V7__drop_favorite_schema.sql
-- The favorite module (com.voyanta.favorite.*) was removed from the codebase.
-- Its functionality is covered by the wishlist module (wishlist.wishlist_items, V3).
-- This migration drops the now-orphaned schema created by V6__favorites.sql.
-- V6 itself is left untouched (existing migrations are never edited).

DROP TABLE IF EXISTS favorite.favorites;
DROP SCHEMA IF EXISTS favorite CASCADE;
