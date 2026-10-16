-- Flyway migration: V4__destination_catalogue.sql
-- Voyanta backend — destination catalogue expansion
--
-- Do not modify V1/V2/V3.
-- image_url is a placeholder (column is NOT NULL); real images
-- are resolved by the backend through Unsplash.
-- Idempotent: skips rows where (name, country) already exists.

INSERT INTO destination.destinations
(id, name, country, image_url, tag, featured, created_at)
SELECT
    gen_random_uuid(),
    v.name,
    v.country,
    'https://placeholder.voyanta.az/destination.jpg',
    v.tag,
    v.featured,
    now() + (row_number() OVER () * interval '1 millisecond')
FROM (VALUES
          -- AZERBAIJAN
          ('Bakı',      'Azərbaycan', 'Şəhər və mədəniyyət',   TRUE),
          ('Qəbələ',    'Azərbaycan', 'Təbiət və dağlar',      TRUE),
          ('Şəki',      'Azərbaycan', 'Tarix və mədəniyyət',   FALSE),
          ('Quba',      'Azərbaycan', 'Təbiət və dağlar',      FALSE),
          ('Qusar',     'Azərbaycan', 'Təbiət və macəra',      FALSE),
          ('Lahıc',     'Azərbaycan', 'Tarix və mədəniyyət',   FALSE),
          ('Naftalan',  'Azərbaycan', 'Sağlamlıq və istirahət', FALSE),
          ('Şuşa',      'Azərbaycan', 'Tarix və mədəniyyət',   TRUE),
          ('Göygöl',    'Azərbaycan', 'Təbiət və dağlar',      FALSE),
          ('Xınalıq',   'Azərbaycan', 'Tarix və mədəniyyət',   FALSE),
          ('Lənkəran',  'Azərbaycan', 'Təbiət və macəra',      FALSE),
          ('Zaqatala',  'Azərbaycan', 'Təbiət və dağlar',      FALSE),

          -- GEORGIA
          ('Tbilisi',   'Gürcüstan',  'Şəhər və mədəniyyət',   FALSE),
          ('Batumi',    'Gürcüstan',  'Dəniz və şəhər',        FALSE),

          -- TURKEY
          ('İstanbul',  'Türkiyə',    'Şəhər və mədəniyyət',   TRUE),
          ('Kapadokya', 'Türkiyə',    'Təbiət və macəra',      TRUE),
          ('Antalya',   'Türkiyə',    'Dəniz və istirahət',    FALSE),
          ('Bodrum',    'Türkiyə',    'Dəniz və istirahət',    FALSE),
          ('İzmir',     'Türkiyə',    'Dəniz və şəhər',        FALSE),
          ('Fethiye',   'Türkiyə',    'Təbiət və macəra',      FALSE),
          ('Marmaris',  'Türkiyə',    'Dəniz və istirahət',    FALSE),
          ('Trabzon',   'Türkiyə',    'Təbiət və dağlar',      FALSE),

          -- EUROPE
          ('Paris',      'Fransa',          'Şəhər və mədəniyyət', TRUE),
          ('Roma',       'İtaliya',         'Tarix və mədəniyyət', FALSE),
          ('London',     'Böyük Britaniya', 'Şəhər və mədəniyyət', FALSE),
          ('Barcelona',  'İspaniya',        'Dəniz və şəhər',      FALSE),
          ('Amsterdam',  'Niderland',       'Şəhər və mədəniyyət', FALSE),
          ('Praqa',      'Çexiya',          'Tarix və mədəniyyət', FALSE),
          ('Vyana',      'Avstriya',        'Şəhər və mədəniyyət', FALSE),
          ('Lissabon',   'Portuqaliya',     'Dəniz və şəhər',      FALSE),
          ('Afina',      'Yunanıstan',      'Tarix və mədəniyyət', FALSE),
          ('Amalfi',     'İtaliya',         'Romantika',           FALSE),
          ('Venesiya',   'İtaliya',         'Romantika',           TRUE),
          ('Budapeşt',   'Macarıstan',      'Şəhər və mədəniyyət', FALSE),
          ('Dubrovnik',  'Xorvatiya',       'Dəniz və şəhər',      FALSE),
          ('Mikonos',    'Yunanıstan',      'Dəniz və istirahət',  FALSE),
          ('İnterlaken', 'İsveçrə',         'Təbiət və dağlar',    FALSE),

          -- ASIA
          ('Tokyo',     'Yaponiya',      'Şəhər və texnologiya', FALSE),
          ('Kyoto',     'Yaponiya',      'Tarix və mədəniyyət',  FALSE),
          ('Seul',      'Cənubi Koreya', 'Şəhər və texnologiya', FALSE),
          ('Bali',      'İndoneziya',    'Dəniz və istirahət',   TRUE),
          ('Banqkok',   'Tayland',       'Şəhər və mədəniyyət',  FALSE),
          ('Phuket',    'Tayland',       'Dəniz və istirahət',   FALSE),
          ('Sinqapur',  'Sinqapur',      'Şəhər və texnologiya', FALSE),

          -- MIDDLE EAST / AFRICA
          ('Dubai',          'BƏƏ',       'Şəhər və lüks',       TRUE),
          ('Qahirə',         'Misir',     'Tarix və mədəniyyət', FALSE),
          ('Hurghada',       'Misir',     'Dəniz və istirahət',  FALSE),
          ('Şarm əl-Şeyx',   'Misir',     'Dəniz və istirahət',  FALSE),
          ('Marrakeş',       'Mərakeş',   'Təbiət və macəra',    FALSE),
          ('Zanzibar',       'Tanzaniya', 'Dəniz və istirahət',  FALSE),

          -- AMERICAS
          ('New York',       'ABŞ',       'Şəhər və əyləncə',    FALSE),
          ('Rio de Janeiro', 'Braziliya', 'Dəniz və şəhər',      FALSE),
          ('Kankun',         'Meksika',   'Dəniz və istirahət',  FALSE)
     ) AS v(name, country, tag, featured)
WHERE NOT EXISTS (
    SELECT 1
    FROM destination.destinations d
    WHERE d.name = v.name
      AND d.country = v.country
);