-- Flyway migration: V5__destination_season.sql
-- Add season column to destinations and backfill with sensible defaults.

-- 1) Add column (nullable first so we can backfill)
ALTER TABLE destination.destinations
    ADD COLUMN IF NOT EXISTS season VARCHAR(32);

-- 2) Backfill based on destination name/country climate
UPDATE destination.destinations
SET season = CASE
    -- Winter destinations
    WHEN name IN ('Kapadokya','İstanbul','Paris','London','Bakı','Tbilisi','Vyana',
                  'Praqa','Budapeşt','Mikonos','Zaqatala','Quba','Lahıc','Naftalan',
                  'Şuşa','Lənkəran','Trabzon','Qusar')
        THEN 'winter'
    -- Spring destinations
    WHEN name IN ('Santorini','Afina','Amalfi','Dubrovnik','Batumi','Amsterdam',
                  'Lissabon','Barcelona','Kankun','Antalya','Bodrum','Marmaris',
                  'Fethiye','İnterlaken','Şəki','Xınalıq','Qəbələ','Göygöl')
        THEN 'spring'
    -- Summer destinations
    WHEN name IN ('Maldiv adaları','Bali','Phuket','Hurghada','Zanzibar','Marrakeş',
                  'Rio de Janeiro','Tokyo','Seul','Sinqapur','Banqkok','New York',
                  'İzmir')
        THEN 'summer'
    -- Autumn destinations
    WHEN name IN ('Dubai','Qahirə','Şarm əl-Şeyx','Kyoto','Roma','Marrakeş')
        THEN 'autumn'
    -- Default
    ELSE 'summer'
END
WHERE season IS NULL;
