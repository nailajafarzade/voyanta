-- Flyway migration: V1__init.sql
-- Voyanta backend — ilkin schema (auth, plan, destination)

-- PG12 və əvvəli üçün ehtiyat — PG13+-da gen_random_uuid() artıq core-dadır,
-- amma bu sətir hər halda zərərsizdir (mövcuddursa heç nə etmir).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS plan;
CREATE SCHEMA IF NOT EXISTS destination;

-- ============================================================
-- auth.users
-- ============================================================
CREATE TABLE auth.users (
                            id             UUID PRIMARY KEY,
                            full_name      VARCHAR(255) NOT NULL,
                            email          VARCHAR(255) NOT NULL UNIQUE,
                            phone          VARCHAR(50),
                            password_hash  VARCHAR(255),          -- Google/Apple istifadəçilərində NULL
                            provider       VARCHAR(20)  NOT NULL, -- LOCAL / GOOGLE / APPLE
                            created_at     TIMESTAMPTZ  NOT NULL
);

-- ============================================================
-- auth.refresh_tokens
-- ============================================================
CREATE TABLE auth.refresh_tokens (
                                     id          UUID PRIMARY KEY,
                                     user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
                                     token_hash  VARCHAR(255) NOT NULL UNIQUE,
                                     expires_at  TIMESTAMPTZ  NOT NULL,
                                     revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
                                     created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON auth.refresh_tokens(user_id);

-- ============================================================
-- plan.travel_plans
-- ============================================================
CREATE TABLE plan.travel_plans (
                                   id                    UUID PRIMARY KEY,
    -- İstifadəçi hesabını silsə plan qalır (SET NULL) — istəsən CASCADE-ə dəyişərik
                                   user_id               UUID REFERENCES auth.users(id) ON DELETE SET NULL,
                                   anonymous_session_id  VARCHAR(100) UNIQUE,
                                   destination           VARCHAR(255),
                                   start_date            DATE,
                                   end_date              DATE,
                                   companion             VARCHAR(20),    -- SOLO / COUPLE / FRIENDS / FAMILY
                                   budget_summary        JSONB,          -- AI generasiyasına qədər NULL
                                   status                VARCHAR(20) NOT NULL, -- GENERATING / READY / FAILED
                                   created_at            TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_travel_plans_user_id ON plan.travel_plans(user_id);

-- ============================================================
-- plan.itinerary_days
-- ============================================================
CREATE TABLE plan.itinerary_days (
                                     id          UUID PRIMARY KEY,
                                     plan_id     UUID NOT NULL REFERENCES plan.travel_plans(id) ON DELETE CASCADE,
                                     day_number  INT  NOT NULL,
                                     items       JSONB,          -- AI generasiyasına qədər NULL
                                     locked      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_itinerary_days_plan_id ON plan.itinerary_days(plan_id);

-- ============================================================
-- destination.destinations
-- ============================================================
CREATE TABLE destination.destinations (
                                          id          UUID PRIMARY KEY,
                                          name        VARCHAR(255) NOT NULL,
                                          country     VARCHAR(255) NOT NULL,
                                          image_url   VARCHAR(500) NOT NULL,
                                          tag         VARCHAR(100) NOT NULL,
                                          featured    BOOLEAN NOT NULL DEFAULT FALSE,
                                          created_at  TIMESTAMPTZ NOT NULL
);

-- Ana səhifədəki 4 populyar kart — seed data.
-- DİQQƏT: image_url-lar placeholder-dır, real S3/R2 linkləri ilə əvəz olunmalıdır.
INSERT INTO destination.destinations (id, name, country, image_url, tag, featured, created_at) VALUES
                                                                                                   (gen_random_uuid(), 'Santorini',       'Yunanıstan',   'https://placeholder.voyanta.az/santorini.jpg', 'Dəniz və gündoğuş',  TRUE, now()),
                                                                                                   (gen_random_uuid(), 'Maldiv adaları',  'Maldivlər',    'https://placeholder.voyanta.az/maldives.jpg',  'Dəniz və gündoğuş',  TRUE, now()),
                                                                                                   (gen_random_uuid(), 'Misir',           'Misir',        'https://placeholder.voyanta.az/egypt.jpg',     'Tarix və mədəniyyət', TRUE, now()),
                                                                                                   (gen_random_uuid(), 'İsmayıllı',       'Azərbaycan',   'https://placeholder.voyanta.az/ismayilli.jpg', 'Təbiət',             TRUE, now());
