-- Flyway migration: V2__personalized_recommendations.sql
-- BE-2 (GET /api/recommendations/personalized) üçün lazım olan struktur data

ALTER TABLE plan.travel_plans
    ADD COLUMN interests JSONB;

ALTER TABLE destination.destinations
    ADD COLUMN interest_tags JSONB;

-- V1-də seed olunan 4 destinasiyaya maraq teqlərini əlavə edirik
UPDATE destination.destinations SET interest_tags = '["SEA"]'::jsonb
WHERE name = 'Santorini';
UPDATE destination.destinations SET interest_tags = '["SEA"]'::jsonb
WHERE name = 'Maldiv adaları';
UPDATE destination.destinations SET interest_tags = '["HISTORY_CULTURE"]'::jsonb
WHERE name = 'Misir';
UPDATE destination.destinations SET interest_tags = '["NATURE"]'::jsonb
WHERE name = 'İsmayıllı';
