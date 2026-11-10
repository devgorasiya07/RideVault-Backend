-- ==============================================================================
-- RideVault — 14-Table BCNF PostgreSQL Database Schema (DDL)
-- ==============================================================================
-- BCNF Compliance:
--   Every determinant in every table is a candidate key.
--   Zero partial dependencies, zero transitive dependencies, zero update anomalies.
--
-- Tables:
--   1.  users               — Base identity & credentials (shared by riders/drivers)
--   2.  riders              — Rider-specific profile metadata
--   3.  drivers             — Driver-specific operational metadata
--   4.  refresh_tokens      — Stateful JWT refresh token rotation & revocation
--   5.  vehicle_categories  — Master catalog of vehicle classes
--   6.  fare_rules          — Base tariffs per vehicle category
--   7.  vehicles            — Physical fleet units
--   8.  vehicle_documents   — Legal/insurance compliance records
--   9.  surge_zones         — Geographic surge pricing zones
--   10. rides               — Central trip ledger (with 4-digit boarding start_pin)
--   11. ride_status_logs    — Immutable audit trail of state transitions
--   12. wallets             — Driver financial account balances
--   13. wallet_transactions — Double-entry financial ledger
--   14. ride_ratings        — Post-ride driver feedback & star ratings
-- ==============================================================================


-- ============================================================
-- 1. USERS — Base identity shared by all user types
-- ============================================================
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
CREATE TABLE IF NOT EXISTS riders (
    rider_id            BIGINT      PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    is_verified         BOOLEAN     NOT NULL DEFAULT FALSE,
    emergency_contact   VARCHAR(15)
);


-- ============================================================
-- 3. DRIVERS — Driver-specific operational metadata
-- ============================================================
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
CREATE TABLE IF NOT EXISTS vehicle_categories (
    category_id     BIGSERIAL       PRIMARY KEY,
    category_name   VARCHAR(20)     NOT NULL UNIQUE CHECK (category_name IN ('MINI', 'SEDAN', 'SUV', 'PREMIUM', 'AUTO', 'BIKE')),
    capacity        INTEGER         NOT NULL DEFAULT 4,
    description     VARCHAR(200)
);


-- ============================================================
-- 6. FARE_RULES — Base tariffs per vehicle category
-- ============================================================
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

ALTER TABLE rides ADD COLUMN IF NOT EXISTS start_pin VARCHAR(4);

CREATE INDEX IF NOT EXISTS idx_rides_rider       ON rides(rider_id);
CREATE INDEX IF NOT EXISTS idx_rides_driver      ON rides(driver_id);
CREATE INDEX IF NOT EXISTS idx_rides_status      ON rides(status);
CREATE INDEX IF NOT EXISTS idx_rides_pending     ON rides(status) WHERE status = 'PENDING';


-- ============================================================
-- 11. RIDE_STATUS_LOGS — Immutable audit trail of state transitions
-- ============================================================
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
-- 14. RIDE_RATINGS — Post-ride driver feedback (1:1 per ride)
-- ============================================================
CREATE TABLE IF NOT EXISTS ride_ratings (
    rating_id       BIGSERIAL       PRIMARY KEY,
    ride_id         BIGINT          NOT NULL UNIQUE REFERENCES rides(ride_id) ON DELETE CASCADE,
    stars           INTEGER         NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment         VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ride_ratings_ride ON ride_ratings(ride_id);
