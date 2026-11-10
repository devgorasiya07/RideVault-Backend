package com.ridevault.service;

import com.ridevault.entity.Ride;
import com.ridevault.entity.RideStatusLog;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.repository.RideRepository;
import com.ridevault.repository.RideStatusLogRepository;
import com.ridevault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled Ride Expiry Job (@Scheduled).
 *
 * Automatically cancels unaccepted PENDING ride requests after the configured timeout
 * (default: 3 minutes) so riders are never stuck indefinitely if no drivers accept.
 * Uses optimistic conditional SQL (WHERE status = 'PENDING') so it never overwrites
 * a ride concurrently claimed by a driver.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideExpiryScheduler {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RideStatusLogRepository statusLogRepository;

    @Value("${application.rides.pending-expiry-minutes:3}")
    private long pendingExpiryMinutes;

    @Scheduled(fixedRateString = "${application.rides.expiry-check-interval-ms:30000}")
    @Transactional
    public void cancelExpiredPendingRides() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pendingExpiryMinutes);
        List<Ride> staleRides = rideRepository.findStaleRidesByStatus(RideStatus.PENDING, cutoff);
        if (staleRides.isEmpty()) {
            return;
        }

        int expiredCount = 0;
        for (Ride stale : staleRides) {
            Long rideId = stale.getRideId();
            Long riderUserId = stale.getRider().getUser().getUserId();

            int updated = rideRepository.cancelRideOptimisticallyIfPending(rideId);
            if (updated == 1) {
                expiredCount++;
                RideStatusLog audit = RideStatusLog.builder()
                        .ride(rideRepository.getReferenceById(rideId))
                        .fromStatus(RideStatus.PENDING)
                        .toStatus(RideStatus.CANCELLED)
                        .changedBy(userRepository.getReferenceById(riderUserId))
                        .changedAt(LocalDateTime.now())
                        .reason("Auto-expired: no driver accepted within " + pendingExpiryMinutes + " minutes")
                        .build();
                statusLogRepository.save(audit);
            }
        }

        if (expiredCount > 0) {
            log.info("Scheduled expiry job auto-cancelled {} stale PENDING ride(s) older than {} minutes.",
                    expiredCount, pendingExpiryMinutes);
        }
    }
}
