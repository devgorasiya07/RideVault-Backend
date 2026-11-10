-- ==============================================================================
-- RideVault — DQL (Data Query Language) 15 Comprehensive Business Queries
-- Target Schema: 14-Table BCNF PostgreSQL Database
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Total Revenue, Ride Count & 20% Commission Breakdown per Vehicle Category
-- ------------------------------------------------------------------------------
SELECT
    vc.category_name                               AS vehicle_category,
    COUNT(r.ride_id)                               AS total_completed_rides,
    ROUND(COALESCE(AVG(r.distance_km), 0), 2)      AS avg_distance_km,
    COALESCE(SUM(r.final_fare), 0.00)              AS total_gross_revenue,
    COALESCE(SUM(r.driver_earning), 0.00)          AS total_driver_payout_80pct,
    COALESCE(SUM(r.platform_commission), 0.00)     AS total_platform_commission_20pct
FROM vehicle_categories vc
LEFT JOIN fare_rules fr       ON fr.category_id = vc.category_id
LEFT JOIN rides r             ON r.fare_id = fr.fare_id AND r.status = 'COMPLETED'
GROUP BY vc.category_name
ORDER BY total_gross_revenue DESC;


-- ------------------------------------------------------------------------------
-- 2. Top 5 Highest Earning Drivers (Leaderboard with Rating & Vehicle Info)
-- ------------------------------------------------------------------------------
SELECT
    d.driver_id,
    CONCAT(u.first_name, ' ', u.last_name)         AS driver_name,
    u.phone_no,
    vc.category_name                               AS vehicle_category,
    v.make_model,
    v.plate_no,
    COUNT(r.ride_id)                               AS completed_rides,
    d.avg_rating,
    COALESCE(w.balance, 0.00)                      AS current_wallet_balance,
    COALESCE(SUM(r.driver_earning), 0.00)          AS total_earnings
FROM drivers d
JOIN users u                  ON u.user_id = d.driver_id
LEFT JOIN vehicles v          ON v.driver_id = d.driver_id AND v.is_active = TRUE
LEFT JOIN vehicle_categories vc ON vc.category_id = v.category_id
LEFT JOIN wallets w           ON w.driver_id = d.driver_id
LEFT JOIN rides r             ON r.driver_id = d.driver_id AND r.status = 'COMPLETED'
GROUP BY d.driver_id, u.first_name, u.last_name, u.phone_no,
         vc.category_name, v.make_model, v.plate_no, d.avg_rating, w.balance
ORDER BY total_earnings DESC, d.avg_rating DESC
LIMIT 5;


-- ------------------------------------------------------------------------------
-- 3. Total Amount Spent by Each Rider (Completed Rides Spend Leaderboard)
-- ------------------------------------------------------------------------------
SELECT
    rd.rider_id,
    CONCAT(u.first_name, ' ', u.last_name)         AS rider_name,
    u.email,
    COUNT(r.ride_id)                               AS rides_taken,
    COALESCE(SUM(r.distance_km), 0.00)             AS total_km_travelled,
    COALESCE(SUM(r.final_fare), 0.00)              AS total_spent
FROM riders rd
JOIN users u                  ON u.user_id = rd.rider_id
JOIN rides r                  ON r.rider_id = rd.rider_id
WHERE r.status = 'COMPLETED'
GROUP BY rd.rider_id, u.first_name, u.last_name, u.email
ORDER BY total_spent DESC;


-- ------------------------------------------------------------------------------
-- 4. Currently Available & Verified Drivers with Active Vehicle Details
-- ------------------------------------------------------------------------------
SELECT
    d.driver_id,
    CONCAT(u.first_name, ' ', u.last_name)         AS driver_name,
    u.phone_no,
    d.license_no,
    vc.category_name                               AS vehicle_category,
    v.make_model,
    v.plate_no,
    d.avg_rating
FROM drivers d
JOIN users u                  ON u.user_id = d.driver_id
JOIN vehicles v               ON v.driver_id = d.driver_id
JOIN vehicle_categories vc    ON vc.category_id = v.category_id
WHERE d.is_available = TRUE
  AND d.is_verified = TRUE
  AND v.is_active = TRUE
  AND v.is_verified = TRUE
ORDER BY d.avg_rating DESC;


-- ------------------------------------------------------------------------------
-- 5. Total Credits & Debits per Driver Wallet (Cashflow Breakdown)
-- ------------------------------------------------------------------------------
SELECT
    w.wallet_id,
    w.driver_id,
    CONCAT(u.first_name, ' ', u.last_name)         AS driver_name,
    w.balance                                      AS current_balance,
    wt.movement_type,
    COUNT(wt.txn_id)                               AS transaction_count,
    SUM(wt.amount)                                 AS total_amount
FROM wallets w
JOIN drivers d                ON d.driver_id = w.driver_id
JOIN users u                  ON u.user_id = d.driver_id
JOIN wallet_transactions wt   ON wt.wallet_id = w.wallet_id
GROUP BY w.wallet_id, w.driver_id, u.first_name, u.last_name, w.balance, wt.movement_type
ORDER BY w.driver_id ASC, wt.movement_type ASC;


-- ------------------------------------------------------------------------------
-- 6. Double-Entry Wallet Reconciliation Audit (Verify Zero Balance Drift)
-- ------------------------------------------------------------------------------
SELECT
    w.wallet_id,
    d.driver_id,
    CONCAT(u.first_name, ' ', u.last_name)         AS driver_name,
    w.balance                                      AS stored_balance,
    COALESCE(SUM(CASE WHEN wt.movement_type = 'CREDIT' THEN wt.amount ELSE 0 END), 0.00) AS total_credits,
    COALESCE(SUM(CASE WHEN wt.movement_type = 'DEBIT'  THEN wt.amount ELSE 0 END), 0.00) AS total_debits,
    w.balance - (
        COALESCE(SUM(CASE WHEN wt.movement_type = 'CREDIT' THEN wt.amount ELSE 0 END), 0.00) -
        COALESCE(SUM(CASE WHEN wt.movement_type = 'DEBIT'  THEN wt.amount ELSE 0 END), 0.00)
    )                                              AS ledger_drift
FROM wallets w
JOIN drivers d                ON d.driver_id = w.driver_id
JOIN users u                  ON u.user_id = d.driver_id
LEFT JOIN wallet_transactions wt ON wt.wallet_id = w.wallet_id
GROUP BY w.wallet_id, d.driver_id, u.first_name, u.last_name, w.balance
ORDER BY ABS(w.balance - (
    COALESCE(SUM(CASE WHEN wt.movement_type = 'CREDIT' THEN wt.amount ELSE 0 END), 0.00) -
    COALESCE(SUM(CASE WHEN wt.movement_type = 'DEBIT'  THEN wt.amount ELSE 0 END), 0.00)
)) DESC, w.wallet_id ASC;


-- ------------------------------------------------------------------------------
-- 7. Cancelled Rides Breakdown with Rider Names, Actor & Audit Reason
-- ------------------------------------------------------------------------------
SELECT
    r.ride_id,
    CONCAT(ru.first_name, ' ', ru.last_name)       AS rider_name,
    r.pickup_address,
    r.dropoff_address,
    r.estimated_fare,
    CONCAT(cu.first_name, ' ', cu.last_name)       AS cancelled_by,
    cu.role                                        AS actor_role,
    l.reason                                       AS cancel_reason,
    r.requested_at,
    l.changed_at                                   AS cancelled_at
FROM rides r
JOIN users ru                 ON ru.user_id = r.rider_id
LEFT JOIN ride_status_logs l  ON l.ride_id = r.ride_id AND l.to_status = 'CANCELLED'
LEFT JOIN users cu            ON cu.user_id = l.changed_by_user_id
WHERE r.status = 'CANCELLED'
ORDER BY l.changed_at DESC NULLS LAST;


-- ------------------------------------------------------------------------------
-- 8. Loyal Riders (Riders with More Than 5 Completed Trips)
-- ------------------------------------------------------------------------------
SELECT
    rd.rider_id,
    CONCAT(u.first_name, ' ', u.last_name)         AS rider_name,
    u.email,
    u.phone_no,
    COUNT(r.ride_id)                               AS total_completed_rides,
    SUM(r.final_fare)                              AS lifetime_spend
FROM riders rd
JOIN users u                  ON u.user_id = rd.rider_id
JOIN rides r                  ON r.rider_id = rd.rider_id
WHERE r.status = 'COMPLETED'
GROUP BY rd.rider_id, u.first_name, u.last_name, u.email, u.phone_no
HAVING COUNT(r.ride_id) > 5
ORDER BY total_completed_rides DESC, lifetime_spend DESC;


-- ------------------------------------------------------------------------------
-- 9. Vehicle Insurance Compliance Watchlist (Expired or Expiring Within 30 Days)
-- ------------------------------------------------------------------------------
SELECT
    v.vehicle_id,
    CONCAT(u.first_name, ' ', u.last_name)         AS driver_name,
    u.phone_no,
    v.make_model,
    v.plate_no,
    vd.insurance_policy_no,
    vd.insurance_expiry_date,
    (vd.insurance_expiry_date - CURRENT_DATE)      AS days_until_expiry,
    CASE
        WHEN vd.insurance_expiry_date < CURRENT_DATE THEN 'EXPIRED'
        ELSE 'EXPIRING_SOON'
    END                                            AS compliance_status
FROM vehicles v
JOIN vehicle_documents vd     ON vd.vehicle_id = v.vehicle_id
JOIN drivers d                ON d.driver_id = v.driver_id
JOIN users u                  ON u.user_id = d.driver_id
WHERE vd.insurance_expiry_date <= (CURRENT_DATE + INTERVAL '30 days')
ORDER BY vd.insurance_expiry_date ASC;


-- ------------------------------------------------------------------------------
-- 10. Completed Rides Pending Post-Ride Customer Rating
-- ------------------------------------------------------------------------------
SELECT
    r.ride_id,
    CONCAT(ru.first_name, ' ', ru.last_name)       AS rider_name,
    CONCAT(du.first_name, ' ', du.last_name)       AS driver_name,
    r.pickup_address,
    r.dropoff_address,
    r.final_fare,
    r.completed_at
FROM rides r
JOIN users ru                 ON ru.user_id = r.rider_id
JOIN users du                 ON du.user_id = r.driver_id
LEFT JOIN ride_ratings rr     ON rr.ride_id = r.ride_id
WHERE r.status = 'COMPLETED'
  AND rr.rating_id IS NULL
ORDER BY r.completed_at DESC;


-- ------------------------------------------------------------------------------
-- 11. Completed Trip Distance, Duration & Fare Analytics
-- ------------------------------------------------------------------------------
SELECT
    COUNT(*)                                       AS completed_trips,
    ROUND(AVG(r.distance_km), 2)                   AS avg_distance_km,
    MAX(r.distance_km)                             AS longest_ride_km,
    MIN(r.distance_km)                             AS shortest_ride_km,
    ROUND(AVG(EXTRACT(EPOCH FROM (r.completed_at - r.accepted_at)) / 60.0), 1) AS avg_trip_minutes,
    ROUND(AVG(r.final_fare), 2)                    AS avg_fare_inr
FROM rides r
WHERE r.status = 'COMPLETED';


-- ------------------------------------------------------------------------------
-- 12. Investigating Low Ratings (2 Stars or Fewer)
-- ------------------------------------------------------------------------------
SELECT
    rr.rating_id,
    r.ride_id,
    CONCAT(ru.first_name, ' ', ru.last_name)       AS rider_name,
    CONCAT(du.first_name, ' ', du.last_name)       AS driver_name,
    v.make_model,
    v.plate_no,
    rr.stars,
    rr.comment,
    rr.created_at
FROM ride_ratings rr
JOIN rides r                  ON r.ride_id = rr.ride_id
JOIN users ru                 ON ru.user_id = r.rider_id
JOIN users du                 ON du.user_id = r.driver_id
LEFT JOIN vehicles v          ON v.vehicle_id = r.vehicle_id
WHERE rr.stars <= 2
ORDER BY rr.stars ASC, rr.created_at DESC;


-- ------------------------------------------------------------------------------
-- 13. Surge Demand & Pricing Multiplier Analysis Across Gandhinagar Hubs
-- ------------------------------------------------------------------------------
SELECT
    r.pickup_address,
    COUNT(*)                                       AS total_requests,
    COUNT(*) FILTER (WHERE r.surge_multiplier > 1.00) AS surge_rides_count,
    COUNT(*) FILTER (WHERE r.status = 'COMPLETED') AS completed_count,
    ROUND(AVG(r.surge_multiplier), 2)              AS avg_surge_multiplier,
    ROUND(AVG(COALESCE(r.final_fare, r.estimated_fare)), 2) AS avg_ticket_fare
FROM rides r
GROUP BY r.pickup_address
ORDER BY total_requests DESC, avg_surge_multiplier DESC;


-- ------------------------------------------------------------------------------
-- 14. Ride Lifecycle State-Machine Audit Trail & Step Transition Latency
-- ------------------------------------------------------------------------------
SELECT
    l.log_id,
    l.ride_id,
    l.from_status,
    l.to_status,
    CONCAT(u.first_name, ' ', u.last_name)         AS changed_by,
    u.role                                         AS actor_role,
    l.changed_at,
    ROUND(EXTRACT(EPOCH FROM (
        l.changed_at - LAG(l.changed_at) OVER (PARTITION BY l.ride_id ORDER BY l.changed_at)
    )), 0)                                         AS seconds_since_prev_step,
    l.reason
FROM ride_status_logs l
JOIN users u ON u.user_id = l.changed_by_user_id
ORDER BY l.ride_id DESC, l.changed_at ASC;


-- ------------------------------------------------------------------------------
-- 15. JWT Refresh Token Session Security Audit
-- ------------------------------------------------------------------------------
SELECT
    rt.token_id,
    u.user_id,
    CONCAT(u.first_name, ' ', u.last_name)         AS account_holder,
    u.email,
    u.role,
    rt.revoked,
    rt.expiry_date,
    CASE
        WHEN rt.revoked = TRUE                     THEN 'REVOKED'
        WHEN rt.expiry_date < CURRENT_TIMESTAMP    THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END                                            AS token_status,
    rt.created_at
FROM refresh_tokens rt
JOIN users u ON u.user_id = rt.user_id
ORDER BY rt.created_at DESC;
