package com.ridevault.entity.enums;

/**
 * Ride lifecycle states enforced by the RideStateMachine.
 *
 * Valid transitions:
 *   PENDING → ACCEPTED → ARRIVED → IN_PROGRESS → COMPLETED
 *   PENDING → CANCELLED  (rider cancels before driver accepts)
 *   ACCEPTED → CANCELLED  (rider/driver cancels after acceptance)
 *   ARRIVED  → CANCELLED  (rider no-show)
 *
 * Resume: "Orchestrated ride state machine workflows"
 * Resume: "raw SQL optimistic locking (WHERE status='PENDING')"
 */
public enum RideStatus {
    PENDING,
    ACCEPTED,
    ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
