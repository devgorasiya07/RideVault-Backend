-- ==============================================================================
-- RideVault 2.0 — 14-Table BCNF PostgreSQL Schema
-- ==============================================================================
-- BCNF Compliance:
--   Every determinant in every table is a candidate key.
--   No partial dependencies, no transitive dependencies, no update anomalies.
--
-- Tables:
--   1.  users               — Base identity & credentials (shared by riders/drivers)
--   2.  riders              — Rider-specific profile metadata
--   3.  drivers             — Driver-specific operational metadata
--   4.  refresh_tokens      — Stateful JWT refresh token revocation
--   5.  vehicle_categories  — Master catalog of vehicle classes
--   6.  fare_rules          — Base tariffs per vehicle category
--   7.  vehicles            — Physical fleet units
--   8.  vehicle_documents   — Legal/insurance compliance records
--   9.  surge_zones         — Geographic surge pricing zones
--   10. rides               — Central trip ledger (with 4-digit start_pin)
--   11. ride_status_logs    — Immutable audit trail of state transitions
--   12. wallets             — Driver financial account balances
--   13. wallet_transactions — Double-entry financial ledger
--   14. ride_ratings        — Post-ride driver feedback & star ratings
-- ==============================================================================


-- ============================================================
-- 1. USERS — Base identity shared by all user types
-- ============================================================
-- BCNF: user_id → all attributes; email and phone_no are also candidate keys.
CREATE TABLE IF NOT EXISTS users (
    user_id         BIGSERIAL       PRIMARY KEY,
    first_name      VARCHAR(50)     NOT NULL,
    middle_name     VARCHAR(50),
    last_name       VARCHAR(50)     NOT NULL,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    phone_no        VARCHAR(15)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL CHECK (role IN ('ROLE_RIDER', 'ROLE_DRIVER', 'ROLE_ADMIN')),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role  ON users(role);


-- ============================================================
-- 2. RIDERS — Rider-specific profile metadata
-- ============================================================
-- BCNF: rider_id → all rider-specific attributes.
-- rider_id is a FK to users.user_id (shared primary key pattern).
CREATE TABLE IF NOT EXISTS riders (
    rider_id            BIGINT      PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    is_verified         BOOLEAN     NOT NULL DEFAULT FALSE,
    emergency_contact   VARCHAR(15)
);


-- ============================================================
-- 3. DRIVERS — Driver-specific operational metadata
-- ============================================================
-- BCNF: driver_id → all driver-specific attributes; license_no is also a candidate key.
CREATE TABLE IF NOT EXISTS drivers (
    driver_id       BIGINT          PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    license_no      VARCHAR(20)     NOT NULL UNIQUE,
    avg_rating      DECIMAL(3,2)    NOT NULL DEFAULT 0.00,
    total_rides     INTEGER         NOT NULL DEFAULT 0,
    is_verified     BOOLEAN         NOT NULL DEFAULT FALSE,
    is_available    BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_drivers_available ON drivers(is_available);


-- ============================================================
-- 4. REFRESH_TOKENS — JWT refresh token management
-- ============================================================
-- BCNF: token_id → all attributes; token_hash is also a candidate key.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    token_id        BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash      VARCHAR(500)    NOT NULL UNIQUE,
    expiry_date     TIMESTAMP       NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);


-- ============================================================
-- 5. VEHICLE_CATEGORIES — Master catalog of vehicle classes
-- ============================================================
-- BCNF: category_id → all attributes; category_name is also a candidate key.
-- Separated from fare_rules to eliminate transitive dependency.
CREATE TABLE IF NOT EXISTS vehicle_categories (
    category_id     BIGSERIAL       PRIMARY KEY,
    category_name   VARCHAR(20)     NOT NULL UNIQUE CHECK (category_name IN ('MINI', 'SEDAN', 'SUV', 'PREMIUM', 'AUTO', 'BIKE')),
    capacity        INTEGER         NOT NULL DEFAULT 4,
    description     VARCHAR(200)
);


-- ============================================================
-- 6. FARE_RULES — Base tariffs per vehicle category
-- ============================================================
-- BCNF: fare_id → all attributes.
-- category_id FK ensures no transitive dependency through vehicle_category string.
CREATE TABLE IF NOT EXISTS fare_rules (
    fare_id         BIGSERIAL       PRIMARY KEY,
    category_id     BIGINT          NOT NULL UNIQUE REFERENCES vehicle_categories(category_id),
    base_fare       DECIMAL(10,2)   NOT NULL,
    per_km_rate     DECIMAL(10,2)   NOT NULL,
    per_min_rate    DECIMAL(10,2)   NOT NULL,
    min_fare        DECIMAL(10,2)   NOT NULL,
    effective_from  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_fare_rules_category_uq ON fare_rules(category_id);


-- ============================================================
-- 7. VEHICLES — Physical fleet units
-- ============================================================
-- BCNF: vehicle_id → all attributes; plate_no is also a candidate key.
-- category_id FK replaces the raw category string from the old schema.
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id          BIGSERIAL       PRIMARY KEY,
    driver_id           BIGINT          NOT NULL REFERENCES drivers(driver_id) ON DELETE CASCADE,
    category_id         BIGINT          NOT NULL REFERENCES vehicle_categories(category_id),
    make_model          VARCHAR(100)    NOT NULL,
    manufacture_year    INTEGER         NOT NULL,
    plate_no            VARCHAR(20)     NOT NULL UNIQUE,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_vehicles_driver   ON vehicles(driver_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_category ON vehicles(category_id);


-- ============================================================
-- 8. VEHICLE_DOCUMENTS — Legal/insurance compliance records
-- ============================================================
-- BCNF: doc_id → all attributes; vehicle_id is also a candidate key (1:1).
-- Separated from vehicles to satisfy BCNF (insurance attributes are
-- functionally dependent on insurance_policy_no, not vehicle_id).
CREATE TABLE IF NOT EXISTS vehicle_documents (
    doc_id                  BIGSERIAL       PRIMARY KEY,
    vehicle_id              BIGINT          NOT NULL UNIQUE REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    insurance_policy_no     VARCHAR(50)     NOT NULL,
    insurance_expiry_date   DATE            NOT NULL,
    rc_number               VARCHAR(50)
);


-- ============================================================
-- 9. SURGE_ZONES — Geographic surge pricing zones
-- ============================================================
-- BCNF: surge_id → all attributes; zone_name is also a candidate key.
CREATE TABLE IF NOT EXISTS surge_zones (
    surge_id        BIGSERIAL       PRIMARY KEY,
    zone_name       VARCHAR(100)    NOT NULL UNIQUE,
    center_lat      DECIMAL(10,6)   NOT NULL,
    center_lng      DECIMAL(10,6)   NOT NULL,
    radius_km       DECIMAL(5,2)    NOT NULL DEFAULT 5.00,
    multiplier      DECIMAL(3,2)    NOT NULL DEFAULT 1.00,
    active_from     TIMESTAMP,
    active_until    TIMESTAMP
);


-- ============================================================
-- 10. RIDES — Central trip ledger
-- ============================================================
-- BCNF: ride_id → all attributes.
-- status column is the target of optimistic locking:
--   UPDATE rides SET ... WHERE ride_id = ? AND status = 'PENDING'
CREATE TABLE IF NOT EXISTS rides (
    ride_id             BIGSERIAL       PRIMARY KEY,
    rider_id            BIGINT          NOT NULL REFERENCES riders(rider_id),
    driver_id           BIGINT          REFERENCES drivers(driver_id),
    vehicle_id          BIGINT          REFERENCES vehicles(vehicle_id),
    fare_id             BIGINT          NOT NULL REFERENCES fare_rules(fare_id),
    pickup_address      VARCHAR(200)    NOT NULL,
    pickup_lat          DECIMAL(10,6)   NOT NULL,
    pickup_lng          DECIMAL(10,6)   NOT NULL,
    dropoff_address     VARCHAR(200)    NOT NULL,
    dropoff_lat         DECIMAL(10,6)   NOT NULL,
    dropoff_lng         DECIMAL(10,6)   NOT NULL,
    distance_km         DECIMAL(10,2),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                        CHECK (status IN ('PENDING', 'ACCEPTED', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    estimated_fare      DECIMAL(10,2)   NOT NULL,
    final_fare          DECIMAL(10,2),
    driver_earning      DECIMAL(10,2),
    platform_commission DECIMAL(10,2),
    surge_multiplier    DECIMAL(3,2)    NOT NULL DEFAULT 1.00,
    start_pin           VARCHAR(4),
    requested_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at         TIMESTAMP,
    completed_at        TIMESTAMP
);

-- Ensure new columns exist on already-initialized databases
ALTER TABLE rides ADD COLUMN IF NOT EXISTS start_pin VARCHAR(4);

CREATE INDEX IF NOT EXISTS idx_rides_rider       ON rides(rider_id);
CREATE INDEX IF NOT EXISTS idx_rides_driver      ON rides(driver_id);
CREATE INDEX IF NOT EXISTS idx_rides_status      ON rides(status);
CREATE INDEX IF NOT EXISTS idx_rides_pending     ON rides(status) WHERE status = 'PENDING';


-- ============================================================
-- 11. RIDE_STATUS_LOGS — Immutable audit trail of state transitions
-- ============================================================
-- BCNF: log_id → all attributes.
-- Resume: "Orchestrated ride state machine workflows"
CREATE TABLE IF NOT EXISTS ride_status_logs (
    log_id              BIGSERIAL       PRIMARY KEY,
    ride_id             BIGINT          NOT NULL REFERENCES rides(ride_id) ON DELETE CASCADE,
    from_status         VARCHAR(20)     NOT NULL,
    to_status           VARCHAR(20)     NOT NULL,
    changed_by_user_id  BIGINT          NOT NULL REFERENCES users(user_id),
    changed_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason              VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_ride_status_logs_ride ON ride_status_logs(ride_id);


-- ============================================================
-- 12. WALLETS — Driver financial account balances
-- ============================================================
-- BCNF: wallet_id → all attributes; driver_id is also a candidate key (1:1).
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id       BIGSERIAL       PRIMARY KEY,
    driver_id       BIGINT          NOT NULL UNIQUE REFERENCES drivers(driver_id) ON DELETE CASCADE,
    balance         DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    currency        VARCHAR(5)      NOT NULL DEFAULT 'INR',
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 13. WALLET_TRANSACTIONS — Double-entry financial ledger
-- ============================================================
-- BCNF: txn_id → all attributes.
-- Resume: "wallet settlements using Spring Core (DI) and @Transactional
--          (ACID rollbacks), guaranteeing 100% financial data consistency"
CREATE TABLE IF NOT EXISTS wallet_transactions (
    txn_id          BIGSERIAL       PRIMARY KEY,
    wallet_id       BIGINT          NOT NULL REFERENCES wallets(wallet_id),
    ride_id         BIGINT          REFERENCES rides(ride_id),
    movement_type   VARCHAR(10)     NOT NULL CHECK (movement_type IN ('CREDIT', 'DEBIT')),
    purpose         VARCHAR(100)    NOT NULL,
    amount          DECIMAL(12,2)   NOT NULL,
    balance_after   DECIMAL(12,2)   NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wallet_txns_wallet ON wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txns_ride   ON wallet_transactions(ride_id);


-- ============================================================
-- 14. RIDE_RATINGS — Post-ride driver feedback (1:1 per completed ride)
-- ============================================================
-- BCNF: rating_id -> all attributes; ride_id is also a candidate key.
-- Strictly BCNF: avoids redundant rider_id/driver_id columns (determined by ride_id).
CREATE TABLE IF NOT EXISTS ride_ratings (
    rating_id       BIGSERIAL       PRIMARY KEY,
    ride_id         BIGINT          NOT NULL UNIQUE REFERENCES rides(ride_id) ON DELETE CASCADE,
    stars           INTEGER         NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment         VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ride_ratings_ride ON ride_ratings(ride_id);


-- ==============================================================================
-- SEED DATA — Vehicle Categories & Fare Rules (Gandhinagar pricing)
-- ==============================================================================

INSERT INTO vehicle_categories (category_name, capacity, description) VALUES
    ('MINI',    4, 'Economy hatchback — Maruti Alto, WagonR, Celerio'),
    ('SEDAN',   4, 'Standard sedan — Swift Dzire, Honda Amaze, Hyundai Aura'),
    ('SUV',     6, 'Full-size SUV — Innova Crysta, XUV700, Scorpio'),
    ('PREMIUM', 4, 'Luxury sedan — BMW 3 Series, Mercedes C-Class, Audi A4'),
    ('AUTO',    3, 'Auto-rickshaw — CNG three-wheeler'),
    ('BIKE',    1, 'Motorcycle — Hero Splendor, Pulsar, Activa')
ON CONFLICT (category_name) DO NOTHING;

INSERT INTO fare_rules (category_id, base_fare, per_km_rate, per_min_rate, min_fare) VALUES
    ((SELECT category_id FROM vehicle_categories WHERE category_name = 'MINI'),    30.00,   9.00,  1.00,  50.00),
    ((SELECT category_id FROM vehicle_categories WHERE category_name = 'SEDAN'),   50.00,  12.00,  1.50,  80.00),
    ((SELECT category_id FROM vehicle_categories WHERE category_name = 'SUV'),     80.00,  16.00,  2.00, 120.00),
    ((SELECT category_id FROM vehicle_categories WHERE category_name = 'PREMIUM'), 150.00,  25.00,  3.00, 200.00),
    ((SELECT category_id FROM vehicle_categories WHERE category_name = 'AUTO'),    20.00,   7.00,  0.75,  35.00),
    ((SELECT category_id FROM vehicle_categories WHERE category_name = 'BIKE'),    15.00,   5.00,  0.50,  25.00)
ON CONFLICT (category_id) DO NOTHING;

-- Gandhinagar surge zones
INSERT INTO surge_zones (zone_name, center_lat, center_lng, radius_km, multiplier) VALUES
    ('Infocity IT Park',           23.1885, 72.6285, 2.00, 1.00),
    ('GIFT City',                  23.1610, 72.6823, 3.00, 1.00),
    ('Gandhinagar Railway Station', 23.2215, 72.6385, 2.50, 1.00),
    ('Mahatma Mandir',             23.2383, 72.6393, 2.00, 1.00),
    ('Akshardham Temple',          23.2295, 72.6739, 2.00, 1.00),
    ('Koba Circle',                23.1515, 72.6315, 3.00, 1.00),
    ('DAIICT Campus',              23.1873, 72.6277, 2.00, 1.00),
    ('PDPU Knowledge Corridor',    23.1554, 72.6672, 2.50, 1.00)
ON CONFLICT (zone_name) DO NOTHING;
