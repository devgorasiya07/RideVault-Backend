-- ==============================================================================
-- RideVault — Master Seed & Realistic Sample Data (DataInsertion.sql)
-- Target Schema: 14-Table BCNF PostgreSQL Database (DDL.sql)
-- Idempotent: Safe to run on freshly initialized databases via ON CONFLICT DO NOTHING
-- Note: Pre-seeded users contain valid 60-character BCrypt hash placeholders for SQL
--       analytics; register fresh demo accounts via the web UI or /api/v1/auth/register/*
--       for live BCrypt password authentication.
-- ==============================================================================

-- ============================================================
-- 1. VEHICLE_CATEGORIES (6 Standard Fleet Classes)
-- ============================================================
INSERT INTO vehicle_categories (category_id, category_name, capacity, description) VALUES
    (1, 'MINI',    4, 'Economy hatchback — Maruti Alto, WagonR, Celerio'),
    (2, 'SEDAN',   4, 'Standard sedan — Swift Dzire, Honda Amaze, Hyundai Aura'),
    (3, 'SUV',     6, 'Full-size SUV — Innova Crysta, XUV700, Scorpio'),
    (4, 'PREMIUM', 4, 'Luxury sedan — BMW 3 Series, Mercedes C-Class, Audi A4'),
    (5, 'AUTO',    3, 'Auto-rickshaw — CNG three-wheeler'),
    (6, 'BIKE',    1, 'Motorcycle — Hero Splendor, Pulsar, Activa')
ON CONFLICT (category_name) DO NOTHING;

-- ============================================================
-- 2. FARE_RULES (1:1 Tariff Configuration per Category)
-- ============================================================
INSERT INTO fare_rules (fare_id, category_id, base_fare, per_km_rate, per_min_rate, min_fare) VALUES
    (1, (SELECT category_id FROM vehicle_categories WHERE category_name = 'MINI'),     30.00,  9.00, 1.00,  50.00),
    (2, (SELECT category_id FROM vehicle_categories WHERE category_name = 'SEDAN'),    50.00, 12.00, 1.50,  80.00),
    (3, (SELECT category_id FROM vehicle_categories WHERE category_name = 'SUV'),      80.00, 16.00, 2.00, 120.00),
    (4, (SELECT category_id FROM vehicle_categories WHERE category_name = 'PREMIUM'), 150.00, 25.00, 3.00, 200.00),
    (5, (SELECT category_id FROM vehicle_categories WHERE category_name = 'AUTO'),     20.00,  7.00, 0.75,  35.00),
    (6, (SELECT category_id FROM vehicle_categories WHERE category_name = 'BIKE'),     15.00,  5.00, 0.50,  25.00)
ON CONFLICT (category_id) DO NOTHING;

-- ============================================================
-- 3. SURGE_ZONES (8 Gandhinagar Hubs, 2 Actively Surging)
-- ============================================================
INSERT INTO surge_zones (surge_id, zone_name, center_lat, center_lng, radius_km, multiplier, active_from, active_until) VALUES
    (1, 'Infocity IT Park',            23.188500, 72.628500, 2.00, 1.40, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '4 hours'),
    (2, 'GIFT City',                   23.161000, 72.682300, 3.00, 1.25, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '3 hours'),
    (3, 'Gandhinagar Railway Station', 23.221500, 72.638500, 2.50, 1.00, NULL, NULL),
    (4, 'Mahatma Mandir',              23.238300, 72.639300, 2.00, 1.00, NULL, NULL),
    (5, 'Akshardham Temple',           23.229500, 72.673900, 2.00, 1.00, NULL, NULL),
    (6, 'Koba Circle',                 23.151500, 72.631500, 3.00, 1.00, NULL, NULL),
    (7, 'DAIICT Campus',               23.187300, 72.627700, 2.00, 1.00, NULL, NULL),
    (8, 'PDPU Knowledge Corridor',     23.155400, 72.667200, 2.50, 1.00, NULL, NULL)
ON CONFLICT (zone_name) DO NOTHING;

-- ============================================================
-- 4. USERS (1 Admin, 8 Riders, 7 Drivers — Shared Base Identity)
-- ============================================================
INSERT INTO users (user_id, first_name, middle_name, last_name, email, phone_no, password_hash, role, is_active, created_at) VALUES
    (1,  'Vikram',    'Singh',   'Mehta',     'admin@ridevault.in',           '+919800000001', '$2a$10$SeedSampleHash000000000000000000000000000000000000001', 'ROLE_ADMIN',  TRUE, CURRENT_TIMESTAMP - INTERVAL '90 days'),
    (2,  'Aarav',     'Kumar',   'Patel',     'aarav.patel@ridevault.in',     '+919876541002', '$2a$10$SeedSampleHash000000000000000000000000000000000000002', 'ROLE_RIDER',  TRUE, CURRENT_TIMESTAMP - INTERVAL '45 days'),
    (3,  'Priya',     NULL,      'Sharma',    'priya.sharma@ridevault.in',    '+919876541003', '$2a$10$SeedSampleHash000000000000000000000000000000000000003', 'ROLE_RIDER',  TRUE, CURRENT_TIMESTAMP - INTERVAL '40 days'),
    (4,  'Rohan',     'Dev',     'Joshi',     'rohan.joshi@ridevault.in',     '+919876541004', '$2a$10$SeedSampleHash000000000000000000000000000000000000004', 'ROLE_RIDER',  TRUE, CURRENT_TIMESTAMP - INTERVAL '35 days'),
    (5,  'Ananya',    NULL,      'Desai',     'ananya.desai@ridevault.in',    '+919876541005', '$2a$10$SeedSampleHash000000000000000000000000000000000000005', 'ROLE_RIDER',  TRUE, CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (6,  'Karan',     'Raj',     'Malhotra',  'karan.malhotra@ridevault.in',  '+919876541006', '$2a$10$SeedSampleHash000000000000000000000000000000000000006', 'ROLE_RIDER',  TRUE, CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (7,  'Neha',      'Kumari',  'Gupta',     'neha.gupta@ridevault.in',      '+919876541007', '$2a$10$SeedSampleHash000000000000000000000000000000000000007', 'ROLE_RIDER',  TRUE, CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (8,  'Diya',      NULL,      'Trivedi',   'diya.trivedi@ridevault.in',    '+919876541008', '$2a$10$SeedSampleHash000000000000000000000000000000000000008', 'ROLE_RIDER',  TRUE, CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (9,  'Siddharth', NULL,      'Menon',     'siddharth.menon@ridevault.in', '+919876541009', '$2a$10$SeedSampleHash000000000000000000000000000000000000009', 'ROLE_RIDER',  TRUE, CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (10, 'Rajesh',    'Kumar',   'Yadav',     'rajesh.driver@ridevault.in',   '+919000001010', '$2a$10$SeedSampleHash000000000000000000000000000000000000010', 'ROLE_DRIVER', TRUE, CURRENT_TIMESTAMP - INTERVAL '60 days'),
    (11, 'Suresh',    'Prasad',  'Pillai',    'suresh.driver@ridevault.in',   '+919000001011', '$2a$10$SeedSampleHash000000000000000000000000000000000000011', 'ROLE_DRIVER', TRUE, CURRENT_TIMESTAMP - INTERVAL '55 days'),
    (12, 'Amit',      'Singh',   'Chauhan',   'amit.driver@ridevault.in',     '+919000001012', '$2a$10$SeedSampleHash000000000000000000000000000000000000012', 'ROLE_DRIVER', TRUE, CURRENT_TIMESTAMP - INTERVAL '50 days'),
    (13, 'Mahesh',    'Bhai',    'Chaudhary', 'mahesh.driver@ridevault.in',   '+919000001013', '$2a$10$SeedSampleHash000000000000000000000000000000000000013', 'ROLE_DRIVER', TRUE, CURRENT_TIMESTAMP - INTERVAL '45 days'),
    (14, 'Sanjay',    'Dutt',    'Sharma',    'sanjay.driver@ridevault.in',   '+919000001014', '$2a$10$SeedSampleHash000000000000000000000000000000000000014', 'ROLE_DRIVER', TRUE, CURRENT_TIMESTAMP - INTERVAL '40 days'),
    (15, 'Ganesh',    'Kumar',   'Iyer',      'ganesh.driver@ridevault.in',   '+919000001015', '$2a$10$SeedSampleHash000000000000000000000000000000000000015', 'ROLE_DRIVER', TRUE, CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (16, 'Prakash',   NULL,      'Solanki',   'prakash.driver@ridevault.in',  '+919000001016', '$2a$10$SeedSampleHash000000000000000000000000000000000000016', 'ROLE_DRIVER', TRUE, CURRENT_TIMESTAMP - INTERVAL '7 days')
ON CONFLICT (user_id) DO NOTHING;

-- ============================================================
-- 5. RIDERS (Shared-PK Specialization of USERS)
-- ============================================================
INSERT INTO riders (rider_id, is_verified, emergency_contact) VALUES
    (2, TRUE,  '+919876500002'),
    (3, TRUE,  '+919876500003'),
    (4, TRUE,  '+919876500004'),
    (5, TRUE,  '+919876500005'),
    (6, TRUE,  '+919876500006'),
    (7, TRUE,  '+919876500007'),
    (8, FALSE, '+919876500008'),
    (9, TRUE,  NULL)
ON CONFLICT (rider_id) DO NOTHING;

-- ============================================================
-- 6. DRIVERS (Shared-PK Specialization of USERS)
--    avg_rating & total_rides strictly match rides + ride_ratings
-- ============================================================
INSERT INTO drivers (driver_id, license_no, avg_rating, total_rides, is_verified, is_available) VALUES
    (10, 'GJ1820200001010', 4.67, 3, TRUE,  TRUE),
    (11, 'GJ1820190001011', 3.50, 2, TRUE,  FALSE),
    (12, 'GJ1820210001012', 5.00, 3, TRUE,  TRUE),
    (13, 'GJ1820180001013', 2.50, 2, TRUE,  FALSE),
    (14, 'GJ1820220001014', 5.00, 2, TRUE,  TRUE),
    (15, 'GJ1820230001015', 4.00, 1, TRUE,  FALSE),
    (16, 'GJ1820240001016', 0.00, 0, FALSE, FALSE)
ON CONFLICT (driver_id) DO NOTHING;

-- ============================================================
-- 7. REFRESH_TOKENS (Stateful JWT Session Rotation Sample Audit)
-- ============================================================
INSERT INTO refresh_tokens (token_id, user_id, token_hash, expiry_date, revoked, created_at) VALUES
    (1, 2,  'sha256_demo_token_hash_rider_aarav_active_001',   CURRENT_TIMESTAMP + INTERVAL '6 days',  FALSE, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (2, 10, 'sha256_demo_token_hash_driver_rajesh_active_002', CURRENT_TIMESTAMP + INTERVAL '5 days',  FALSE, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 3,  'sha256_demo_token_hash_rider_priya_rotated_003', CURRENT_TIMESTAMP + INTERVAL '3 days',  TRUE,  CURRENT_TIMESTAMP - INTERVAL '4 days')
ON CONFLICT (token_id) DO NOTHING;

-- ============================================================
-- 8. VEHICLES (Fleet Units Assigned to Drivers)
-- ============================================================
INSERT INTO vehicles (vehicle_id, driver_id, category_id, make_model, manufacture_year, plate_no, is_active, is_verified) VALUES
    (1, 10, (SELECT category_id FROM vehicle_categories WHERE category_name = 'SEDAN'),   'Honda Amaze',      2022, 'GJ-18-AB-1010', TRUE,  TRUE),
    (2, 11, (SELECT category_id FROM vehicle_categories WHERE category_name = 'MINI'),    'Maruti WagonR',    2021, 'GJ-18-CD-1011', TRUE,  TRUE),
    (3, 12, (SELECT category_id FROM vehicle_categories WHERE category_name = 'SUV'),     'Toyota Innova',    2023, 'GJ-18-EF-1012', TRUE,  TRUE),
    (4, 13, (SELECT category_id FROM vehicle_categories WHERE category_name = 'AUTO'),    'Bajaj RE CNG',     2020, 'GJ-18-AU-1013', TRUE,  TRUE),
    (5, 14, (SELECT category_id FROM vehicle_categories WHERE category_name = 'PREMIUM'), 'BMW 3 Series',     2023, 'GJ-18-PR-1014', TRUE,  TRUE),
    (6, 15, (SELECT category_id FROM vehicle_categories WHERE category_name = 'BIKE'),    'Hero Splendor+',   2022, 'GJ-18-BK-1015', TRUE,  TRUE),
    (7, 16, (SELECT category_id FROM vehicle_categories WHERE category_name = 'MINI'),    'Tata Tiago',       2019, 'GJ-18-GH-1016', FALSE, FALSE)
ON CONFLICT (vehicle_id) DO NOTHING;

-- ============================================================
-- 9. VEHICLE_DOCUMENTS (Insurance & RC Compliance; 3 Alert Cases)
-- ============================================================
INSERT INTO vehicle_documents (doc_id, vehicle_id, insurance_policy_no, insurance_expiry_date, rc_number) VALUES
    (1, 1, 'INS-GJ-2026-1010', CURRENT_DATE + INTERVAL '240 days', 'RC-GJ18-1010'),
    (2, 2, 'INS-GJ-2026-1011', CURRENT_DATE + INTERVAL '14 days',  'RC-GJ18-1011'),
    (3, 3, 'INS-GJ-2026-1012', CURRENT_DATE + INTERVAL '180 days', 'RC-GJ18-1012'),
    (4, 4, 'INS-GJ-2026-1013', CURRENT_DATE + INTERVAL '22 days',  'RC-GJ18-1013'),
    (5, 5, 'INS-GJ-2026-1014', CURRENT_DATE + INTERVAL '320 days', 'RC-GJ18-1014'),
    (6, 6, 'INS-GJ-2026-1015', CURRENT_DATE + INTERVAL '95 days',  'RC-GJ18-1015'),
    (7, 7, 'INS-GJ-2025-1016', CURRENT_DATE - INTERVAL '5 days',   'RC-GJ18-1016')
ON CONFLICT (vehicle_id) DO NOTHING;

-- ============================================================
-- 10. RIDES (19 Trips Across All 6 Lifecycle States & Categories)
--     Rider 2 (Aarav) has 6 COMPLETED rides; 1 ride per active state
-- ============================================================
INSERT INTO rides (ride_id, rider_id, driver_id, vehicle_id, fare_id, pickup_address, pickup_lat, pickup_lng, dropoff_address, dropoff_lat, dropoff_lng, distance_km, status, estimated_fare, final_fare, driver_earning, platform_commission, surge_multiplier, start_pin, requested_at, accepted_at, completed_at) VALUES
    (1,  2, 10, 1, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'SEDAN'),   'Infocity IT Park',            23.188500, 72.628500, 'GIFT City',                   23.161000, 72.682300,  8.50, 'COMPLETED',   250.00, 250.00, 200.00,  50.00, 1.40, '4821', CURRENT_TIMESTAMP - INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '2 minutes', CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '28 minutes'),
    (2,  2, 10, 1, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'SEDAN'),   'DAIICT Campus',               23.187300, 72.627700, 'Mahatma Mandir',              23.238300, 72.639300,  7.20, 'COMPLETED',   180.00, 180.00, 144.00,  36.00, 1.00, '7319', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '1 minute',  CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '21 minutes'),
    (3,  2, 11, 2, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'MINI'),    'Gandhinagar Railway Station', 23.221500, 72.638500, 'Akshardham Temple',           23.229500, 72.673900,  6.40, 'COMPLETED',   150.00, 150.00, 120.00,  30.00, 1.00, '5904', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '2 minutes', CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '19 minutes'),
    (4,  2, 12, 3, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'SUV'),     'GIFT City',                   23.161000, 72.682300, 'Koba Circle',                 23.151500, 72.631500, 11.20, 'COMPLETED',   450.00, 450.00, 360.00,  90.00, 1.25, '2167', CURRENT_TIMESTAMP - INTERVAL '9 days',  CURRENT_TIMESTAMP - INTERVAL '9 days'  + INTERVAL '3 minutes', CURRENT_TIMESTAMP - INTERVAL '9 days'  + INTERVAL '32 minutes'),
    (5,  2, 13, 4, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'AUTO'),    'Sector 11 Market',            23.218000, 72.645000, 'DAIICT Campus',               23.187300, 72.627700,  5.10, 'COMPLETED',    90.00,  90.00,  72.00,  18.00, 1.00, '8842', CURRENT_TIMESTAMP - INTERVAL '8 days',  CURRENT_TIMESTAMP - INTERVAL '8 days'  + INTERVAL '2 minutes', CURRENT_TIMESTAMP - INTERVAL '8 days'  + INTERVAL '18 minutes'),
    (6,  2, 14, 5, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'PREMIUM'), 'Infocity IT Park',            23.188500, 72.628500, 'Mahatma Mandir',              23.238300, 72.639300, 10.60, 'COMPLETED',   650.00, 650.00, 520.00, 130.00, 1.40, '3390', CURRENT_TIMESTAMP - INTERVAL '7 days',  CURRENT_TIMESTAMP - INTERVAL '7 days'  + INTERVAL '1 minute',  CURRENT_TIMESTAMP - INTERVAL '7 days'  + INTERVAL '27 minutes'),
    (7,  3, 10, 1, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'SEDAN'),   'GIFT City',                   23.161000, 72.682300, 'Gandhinagar Railway Station', 23.221500, 72.638500, 12.00, 'COMPLETED',   300.00, 300.00, 240.00,  60.00, 1.25, '6012', CURRENT_TIMESTAMP - INTERVAL '6 days',  CURRENT_TIMESTAMP - INTERVAL '6 days'  + INTERVAL '2 minutes', CURRENT_TIMESTAMP - INTERVAL '6 days'  + INTERVAL '30 minutes'),
    (8,  3, 11, 2, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'MINI'),    'PDPU Knowledge Corridor',     23.155400, 72.667200, 'Infocity IT Park',            23.188500, 72.628500,  6.80, 'COMPLETED',   120.00, 120.00,  96.00,  24.00, 1.00, '9471', CURRENT_TIMESTAMP - INTERVAL '5 days',  CURRENT_TIMESTAMP - INTERVAL '5 days'  + INTERVAL '4 minutes', CURRENT_TIMESTAMP - INTERVAL '5 days'  + INTERVAL '24 minutes'),
    (9,  3, 15, 6, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'BIKE'),    'Akshardham Temple',           23.229500, 72.673900, 'Sector 21 Shopping Center',   23.235000, 72.652000,  3.20, 'CANCELLED',    45.00,   NULL,   NULL,   NULL, 1.00, '1185', CURRENT_TIMESTAMP - INTERVAL '5 days',  CURRENT_TIMESTAMP - INTERVAL '5 days'  + INTERVAL '1 minute',  NULL),
    (10, 4, 12, 3, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'SUV'),     'Mahatma Mandir',              23.238300, 72.639300, 'GIFT City',                   23.161000, 72.682300, 14.50, 'COMPLETED',   500.00, 500.00, 400.00, 100.00, 1.00, '7730', CURRENT_TIMESTAMP - INTERVAL '4 days',  CURRENT_TIMESTAMP - INTERVAL '4 days'  + INTERVAL '2 minutes', CURRENT_TIMESTAMP - INTERVAL '4 days'  + INTERVAL '36 minutes'),
    (11, 4, 12, 3, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'SUV'),     'Infocity IT Park',            23.188500, 72.628500, 'Koba Circle',                 23.151500, 72.631500,  8.10, 'COMPLETED',   350.00, 350.00, 280.00,  70.00, 1.40, '4420', CURRENT_TIMESTAMP - INTERVAL '3 days',  CURRENT_TIMESTAMP - INTERVAL '3 days'  + INTERVAL '2 minutes', CURRENT_TIMESTAMP - INTERVAL '3 days'  + INTERVAL '22 minutes'),
    (12, 4, NULL, NULL, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'MINI'), 'Koba Circle',                23.151500, 72.631500, 'DAIICT Campus',               23.187300, 72.627700,  5.50, 'CANCELLED',   110.00,   NULL,   NULL,   NULL, 1.00, '5509', CURRENT_TIMESTAMP - INTERVAL '3 days',  NULL, NULL),
    (13, 5, 13, 4, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'AUTO'),    'DAIICT Campus',               23.187300, 72.627700, 'Gandhinagar Railway Station', 23.221500, 72.638500,  4.80, 'COMPLETED',    75.00,  75.00,  60.00,  15.00, 1.00, '2918', CURRENT_TIMESTAMP - INTERVAL '2 days',  CURRENT_TIMESTAMP - INTERVAL '2 days'  + INTERVAL '2 minutes', CURRENT_TIMESTAMP - INTERVAL '2 days'  + INTERVAL '17 minutes'),
    (14, 5, 14, 5, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'PREMIUM'), 'GIFT City',                   23.161000, 72.682300, 'Akshardham Temple',           23.229500, 72.673900, 15.20, 'COMPLETED',   800.00, 800.00, 640.00, 160.00, 1.25, '8154', CURRENT_TIMESTAMP - INTERVAL '1 day',   CURRENT_TIMESTAMP - INTERVAL '1 day'   + INTERVAL '1 minute',  CURRENT_TIMESTAMP - INTERVAL '1 day'   + INTERVAL '31 minutes'),
    (15, 6, 11, 2, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'MINI'),    'Infocity IT Park',            23.188500, 72.628500, 'PDPU Knowledge Corridor',     23.155400, 72.667200,  7.40, 'IN_PROGRESS', 140.00,   NULL,   NULL,   NULL, 1.40, '3672', CURRENT_TIMESTAMP - INTERVAL '18 minutes', CURRENT_TIMESTAMP - INTERVAL '15 minutes', NULL),
    (16, 6, 13, 4, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'AUTO'),    'Gandhinagar Railway Station', 23.221500, 72.638500, 'Mahatma Mandir',              23.238300, 72.639300,  4.20, 'ARRIVED',      85.00,   NULL,   NULL,   NULL, 1.00, '6248', CURRENT_TIMESTAMP - INTERVAL '10 minutes', CURRENT_TIMESTAMP - INTERVAL '8 minutes',  NULL),
    (17, 7, 15, 6, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'BIKE'),    'DAIICT Campus',               23.187300, 72.627700, 'Infocity IT Park',            23.188500, 72.628500,  2.10, 'ACCEPTED',     35.00,   NULL,   NULL,   NULL, 1.00, '7093', CURRENT_TIMESTAMP - INTERVAL '6 minutes',  CURRENT_TIMESTAMP - INTERVAL '4 minutes',  NULL),
    (18, 8, NULL, NULL, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'SEDAN'), 'Infocity IT Park',          23.188500, 72.628500, 'GIFT City',                   23.161000, 72.682300,  8.50, 'PENDING',     215.00,   NULL,   NULL,   NULL, 1.40, '1950', CURRENT_TIMESTAMP - INTERVAL '1 minute',   NULL, NULL),
    (19, 7, 15, 6, (SELECT fare_id FROM fare_rules fr JOIN vehicle_categories vc ON fr.category_id = vc.category_id WHERE vc.category_name = 'BIKE'),    'PDPU Knowledge Corridor',     23.155400, 72.667200, 'DAIICT Campus',               23.187300, 72.627700,  5.00, 'COMPLETED',    50.00,  50.00,  40.00,  10.00, 1.00, '4029', CURRENT_TIMESTAMP - INTERVAL '2 days',  CURRENT_TIMESTAMP - INTERVAL '2 days'  + INTERVAL '1 minute',  CURRENT_TIMESTAMP - INTERVAL '2 days'  + INTERVAL '14 minutes')
ON CONFLICT (ride_id) DO NOTHING;

-- ============================================================
-- 11. RIDE_STATUS_LOGS (Immutable Transition Audit History)
-- ============================================================
INSERT INTO ride_status_logs (log_id, ride_id, from_status, to_status, changed_by_user_id, changed_at, reason) VALUES
    (1,  1, 'PENDING',     'PENDING',     2,  CURRENT_TIMESTAMP - INTERVAL '14 days',                        'Ride requested by rider'),
    (2,  1, 'PENDING',     'ACCEPTED',    10, CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '2 minutes', 'Driver accepted ride'),
    (3,  1, 'ACCEPTED',    'ARRIVED',     10, CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '6 minutes', 'Driver arrived at pickup'),
    (4,  1, 'ARRIVED',     'IN_PROGRESS', 10, CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '7 minutes', '4-digit start PIN verified'),
    (5,  1, 'IN_PROGRESS', 'COMPLETED',   10, CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '28 minutes','Trip completed & wallet settled'),
    (6,  8, 'PENDING',     'PENDING',     3,  CURRENT_TIMESTAMP - INTERVAL '5 days',                         'Ride requested by rider'),
    (7,  8, 'PENDING',     'ACCEPTED',    11, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '4 minutes',  'Driver accepted ride'),
    (8,  8, 'ACCEPTED',    'ARRIVED',     11, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '14 minutes', 'Driver arrived late at PDPU'),
    (9,  8, 'ARRIVED',     'IN_PROGRESS', 11, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '15 minutes', '4-digit start PIN verified'),
    (10, 8, 'IN_PROGRESS', 'COMPLETED',   11, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '24 minutes', 'Trip completed & wallet settled'),
    (11, 9, 'PENDING',     'PENDING',     3,  CURRENT_TIMESTAMP - INTERVAL '5 days',                         'Ride requested by rider'),
    (12, 9, 'PENDING',     'ACCEPTED',    15, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '1 minute',   'Driver accepted ride'),
    (13, 9, 'ACCEPTED',    'CANCELLED',   3,  CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '5 minutes',  'Rider cancelled due to rain'),
    (14, 12,'PENDING',     'PENDING',     4,  CURRENT_TIMESTAMP - INTERVAL '3 days',                         'Ride requested by rider'),
    (15, 12,'PENDING',     'CANCELLED',   1,  CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '3 minutes',  'Auto-expired after 3 minutes with no driver'),
    (16, 15,'PENDING',     'PENDING',     6,  CURRENT_TIMESTAMP - INTERVAL '18 minutes',                     'Ride requested by rider'),
    (17, 15,'PENDING',     'ACCEPTED',    11, CURRENT_TIMESTAMP - INTERVAL '15 minutes',                     'Driver accepted ride'),
    (18, 15,'ACCEPTED',    'ARRIVED',     11, CURRENT_TIMESTAMP - INTERVAL '11 minutes',                     'Driver arrived at Infocity'),
    (19, 15,'ARRIVED',     'IN_PROGRESS', 11, CURRENT_TIMESTAMP - INTERVAL '10 minutes',                     '4-digit start PIN verified'),
    (20, 16,'PENDING',     'PENDING',     6,  CURRENT_TIMESTAMP - INTERVAL '10 minutes',                     'Ride requested by rider'),
    (21, 16,'PENDING',     'ACCEPTED',    13, CURRENT_TIMESTAMP - INTERVAL '8 minutes',                      'Driver accepted ride'),
    (22, 16,'ACCEPTED',    'ARRIVED',     13, CURRENT_TIMESTAMP - INTERVAL '2 minutes',                      'Driver arrived at Railway Station'),
    (23, 17,'PENDING',     'PENDING',     7,  CURRENT_TIMESTAMP - INTERVAL '6 minutes',                      'Ride requested by rider'),
    (24, 17,'PENDING',     'ACCEPTED',    15, CURRENT_TIMESTAMP - INTERVAL '4 minutes',                      'Driver accepted ride'),
    (25, 18,'PENDING',     'PENDING',     8,  CURRENT_TIMESTAMP - INTERVAL '1 minute',                       'Ride awaiting driver dispatch')
ON CONFLICT (log_id) DO NOTHING;

-- ============================================================
-- 12. WALLETS (Driver Accounts — Exact Balance = Credits - Debits)
-- ============================================================
INSERT INTO wallets (wallet_id, driver_id, balance, currency, updated_at) VALUES
    (1, 10, 384.00, 'INR', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (2, 11, 216.00, 'INR', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (3, 12, 540.00, 'INR', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (4, 13, 132.00, 'INR', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (5, 14, 560.00, 'INR', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (6, 15,  40.00, 'INR', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (7, 16,   0.00, 'INR', CURRENT_TIMESTAMP - INTERVAL '7 days')
ON CONFLICT (driver_id) DO NOTHING;

-- ============================================================
-- 13. WALLET_TRANSACTIONS (Double-Entry Ledger — Zero Drift)
-- ============================================================
INSERT INTO wallet_transactions (txn_id, wallet_id, ride_id, movement_type, purpose, amount, balance_after, created_at) VALUES
    (1,  1, 1,    'CREDIT', 'Ride Earning (80% split)',     200.00,  200.00, CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '28 minutes'),
    (2,  1, 2,    'CREDIT', 'Ride Earning (80% split)',     144.00,  344.00, CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '21 minutes'),
    (3,  1, 7,    'CREDIT', 'Ride Earning (80% split)',     240.00,  584.00, CURRENT_TIMESTAMP - INTERVAL '6 days'  + INTERVAL '30 minutes'),
    (4,  1, NULL, 'DEBIT',  'Driver Bank Withdrawal (IMPS)',200.00,  384.00, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (5,  2, 3,    'CREDIT', 'Ride Earning (80% split)',     120.00,  120.00, CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '19 minutes'),
    (6,  2, 8,    'CREDIT', 'Ride Earning (80% split)',      96.00,  216.00, CURRENT_TIMESTAMP - INTERVAL '5 days'  + INTERVAL '24 minutes'),
    (7,  3, 4,    'CREDIT', 'Ride Earning (80% split)',     360.00,  360.00, CURRENT_TIMESTAMP - INTERVAL '9 days'  + INTERVAL '32 minutes'),
    (8,  3, 10,   'CREDIT', 'Ride Earning (80% split)',     400.00,  760.00, CURRENT_TIMESTAMP - INTERVAL '4 days'  + INTERVAL '36 minutes'),
    (9,  3, 11,   'CREDIT', 'Ride Earning (80% split)',     280.00, 1040.00, CURRENT_TIMESTAMP - INTERVAL '3 days'  + INTERVAL '22 minutes'),
    (10, 3, NULL, 'DEBIT',  'Driver Bank Withdrawal (UPI)', 500.00,  540.00, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (11, 4, 5,    'CREDIT', 'Ride Earning (80% split)',      72.00,   72.00, CURRENT_TIMESTAMP - INTERVAL '8 days'  + INTERVAL '18 minutes'),
    (12, 4, 13,   'CREDIT', 'Ride Earning (80% split)',      60.00,  132.00, CURRENT_TIMESTAMP - INTERVAL '2 days'  + INTERVAL '17 minutes'),
    (13, 5, 6,    'CREDIT', 'Ride Earning (80% split)',     520.00,  520.00, CURRENT_TIMESTAMP - INTERVAL '7 days'  + INTERVAL '27 minutes'),
    (14, 5, 14,   'CREDIT', 'Ride Earning (80% split)',     640.00, 1160.00, CURRENT_TIMESTAMP - INTERVAL '1 day'   + INTERVAL '31 minutes'),
    (15, 5, NULL, 'DEBIT',  'Driver Bank Withdrawal (NEFT)',600.00,  560.00, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (16, 6, 19,   'CREDIT', 'Ride Earning (80% split)',      40.00,   40.00, CURRENT_TIMESTAMP - INTERVAL '2 days'  + INTERVAL '14 minutes')
ON CONFLICT (txn_id) DO NOTHING;

-- ============================================================
-- 14. RIDE_RATINGS (Strict BCNF 1:1 Feedback; Rides 11 & 14 Unrated)
-- ============================================================
INSERT INTO ride_ratings (rating_id, ride_id, stars, comment, created_at) VALUES
    (1,  1,  5, 'Very smooth highway drive near Infocity.',        CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '35 minutes'),
    (2,  2,  4, 'Good pickup time and clean sedan.',              CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '25 minutes'),
    (3,  3,  5, 'Polite driver and exact fare.',                  CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '25 minutes'),
    (4,  4,  5, 'Spacious SUV for group commute to GIFT City.',   CURRENT_TIMESTAMP - INTERVAL '9 days'  + INTERVAL '40 minutes'),
    (5,  5,  4, 'Quick auto ride across Sector 11.',              CURRENT_TIMESTAMP - INTERVAL '8 days'  + INTERVAL '25 minutes'),
    (6,  6,  5, 'Premium executive sedan experience.',            CURRENT_TIMESTAMP - INTERVAL '7 days'  + INTERVAL '35 minutes'),
    (7,  7,  5, 'Safe driving and followed shortest route.',      CURRENT_TIMESTAMP - INTERVAL '6 days'  + INTERVAL '38 minutes'),
    (8,  8,  2, 'AC was weak and pickup arrived 10 mins late.',   CURRENT_TIMESTAMP - INTERVAL '5 days'  + INTERVAL '30 minutes'),
    (9,  10, 5, 'Spotless interior, great trip.',                 CURRENT_TIMESTAMP - INTERVAL '4 days'  + INTERVAL '42 minutes'),
    (10, 13, 1, 'Driver took a longer detour without asking.',    CURRENT_TIMESTAMP - INTERVAL '2 days'  + INTERVAL '22 minutes'),
    (11, 19, 4, 'Fast bike pickup during evening traffic.',       CURRENT_TIMESTAMP - INTERVAL '2 days'  + INTERVAL '20 minutes')
ON CONFLICT (ride_id) DO NOTHING;

-- ============================================================
-- SYNCHRONIZE SERIAL SEQUENCES FOR SAFE LIVE SPRING BOOT INSERTS
-- ============================================================
SELECT setval(pg_get_serial_sequence('vehicle_categories', 'category_id'), COALESCE((SELECT MAX(category_id) FROM vehicle_categories), 1));
SELECT setval(pg_get_serial_sequence('fare_rules',         'fare_id'),     COALESCE((SELECT MAX(fare_id)     FROM fare_rules), 1));
SELECT setval(pg_get_serial_sequence('surge_zones',        'surge_id'),    COALESCE((SELECT MAX(surge_id)    FROM surge_zones), 1));
SELECT setval(pg_get_serial_sequence('users',              'user_id'),     COALESCE((SELECT MAX(user_id)     FROM users), 1));
SELECT setval(pg_get_serial_sequence('refresh_tokens',     'token_id'),    COALESCE((SELECT MAX(token_id)    FROM refresh_tokens), 1));
SELECT setval(pg_get_serial_sequence('vehicles',           'vehicle_id'),  COALESCE((SELECT MAX(vehicle_id)  FROM vehicles), 1));
SELECT setval(pg_get_serial_sequence('vehicle_documents',  'doc_id'),      COALESCE((SELECT MAX(doc_id)      FROM vehicle_documents), 1));
SELECT setval(pg_get_serial_sequence('rides',              'ride_id'),     COALESCE((SELECT MAX(ride_id)     FROM rides), 1));
SELECT setval(pg_get_serial_sequence('ride_status_logs',   'log_id'),      COALESCE((SELECT MAX(log_id)      FROM ride_status_logs), 1));
SELECT setval(pg_get_serial_sequence('wallets',            'wallet_id'),   COALESCE((SELECT MAX(wallet_id)   FROM wallets), 1));
SELECT setval(pg_get_serial_sequence('wallet_transactions','txn_id'),      COALESCE((SELECT MAX(txn_id)      FROM wallet_transactions), 1));
SELECT setval(pg_get_serial_sequence('ride_ratings',       'rating_id'),   COALESCE((SELECT MAX(rating_id)   FROM ride_ratings), 1));
