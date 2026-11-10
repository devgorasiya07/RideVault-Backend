package com.ridevault.service.statemachine;

import com.ridevault.entity.Ride;
import com.ridevault.entity.RideStatusLog;
import com.ridevault.entity.User;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.exception.InvalidRideStateException;
import com.ridevault.repository.RideStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Ride State Machine
 *
 * Resume: "Orchestrated ride state machine workflows and wallet settlements using
 *          Spring Core (DI) and @Transactional (ACID rollbacks)"
 *
 * Enforces valid state transitions and records immutable audit logs for every state change:
 *   PENDING     → ACCEPTED, CANCELLED
 *   ACCEPTED    → ARRIVED, CANCELLED
 *   ARRIVED     → IN_PROGRESS, CANCELLED
 *   IN_PROGRESS → COMPLETED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RideStateMachine {

    private final RideStatusLogRepository statusLogRepository;

    // Directed graph of allowable state transitions
    private static final Map<RideStatus, Set<RideStatus>> ALLOWED_TRANSITIONS = Map.of(
            RideStatus.PENDING, Set.of(RideStatus.ACCEPTED, RideStatus.CANCELLED),
            RideStatus.ACCEPTED, Set.of(RideStatus.ARRIVED, RideStatus.CANCELLED),
            RideStatus.ARRIVED, Set.of(RideStatus.IN_PROGRESS, RideStatus.CANCELLED),
            RideStatus.IN_PROGRESS, Set.of(RideStatus.COMPLETED),
            RideStatus.COMPLETED, Set.of(), // Terminal state
            RideStatus.CANCELLED, Set.of()  // Terminal state
    );

    /**
     * Validates and transitions a ride from its current status to target status.
     *
     * @param ride The ride entity
     * @param targetStatus The requested new status
     * @param changedBy The user initiating the transition
     * @param reason Optional human-readable reason
     */
    public void transition(Ride ride, RideStatus targetStatus, User changedBy, String reason) {
        RideStatus currentStatus = ride.getStatus();

        log.info("Attempting state transition for Ride ID {}: {} -> {}",
                ride.getRideId(), currentStatus, targetStatus);

        Set<RideStatus> validNextStates = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!validNextStates.contains(targetStatus)) {
            String errorMsg = String.format(
                    "Invalid state transition: Cannot transition Ride [%d] from '%s' to '%s'. Valid transitions from '%s' are: %s",
                    ride.getRideId(), currentStatus, targetStatus, currentStatus, validNextStates
            );
            log.warn(errorMsg);
            throw new InvalidRideStateException(errorMsg);
        }

        // Apply state transition
        ride.setStatus(targetStatus);

        if (targetStatus == RideStatus.ACCEPTED) {
            ride.setAcceptedAt(LocalDateTime.now());
        } else if (targetStatus == RideStatus.COMPLETED) {
            ride.setCompletedAt(LocalDateTime.now());
        }

        // Record immutable audit log
        RideStatusLog auditLog = RideStatusLog.builder()
                .ride(ride)
                .fromStatus(currentStatus)
                .toStatus(targetStatus)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .reason(reason)
                .build();
        statusLogRepository.save(auditLog);

        log.info("Successfully transitioned Ride ID {} to status: {}", ride.getRideId(), targetStatus);
    }

    /**
     * Checks if a transition is valid without throwing an exception.
     */
    public boolean canTransition(RideStatus currentStatus, RideStatus targetStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }
}
