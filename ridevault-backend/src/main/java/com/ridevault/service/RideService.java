package com.ridevault.service;

import com.ridevault.dto.request.RateRideRequest;
import com.ridevault.dto.request.RideBookingRequest;
import com.ridevault.dto.response.PageResponse;
import com.ridevault.dto.response.RideResponse;
import com.ridevault.dto.response.RideStatusLogResponse;
import com.ridevault.entity.*;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.exception.ResourceNotFoundException;
import com.ridevault.exception.RideAlreadyClaimedException;
import com.ridevault.repository.*;
import com.ridevault.service.statemachine.RideStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ride Booking & Lifecycle Management Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final RiderRepository riderRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final FareRuleRepository fareRuleRepository;
    private final PricingService pricingService;
    private final RideStateMachine stateMachine;
    private final RideStatusLogRepository statusLogRepository;
    private final RideRatingRepository rideRatingRepository;
    private final WalletSettlementService settlementService;

    private final SecureRandom pinRandom = new SecureRandom();

    // ========================================================================
    //  1. Book a New Ride (Rider Portal) — generates 4-digit Start PIN
    // ========================================================================
    @Transactional
    public RideResponse bookRide(Long riderId, RideBookingRequest request) {
        log.info("Booking new ride for rider ID: {}, category: {}", riderId, request.getVehicleCategory());

        if (request.getPickupAddress().trim().equalsIgnoreCase(request.getDropoffAddress().trim())
                && Double.compare(request.getPickupLat(), request.getDropoffLat()) == 0
                && Double.compare(request.getPickupLng(), request.getDropoffLng()) == 0) {
            throw new IllegalArgumentException("Pickup and drop-off locations cannot be identical.");
        }

        Rider rider = riderRepository.findById(riderId)
                .orElseGet(() -> {
                    User user = userRepository.findById(riderId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + riderId));
                    return riderRepository.save(Rider.builder().user(user).isVerified(true).build());
                });

        rideRepository.findActiveRideByRider(rider).ifPresent(r -> {
            throw new IllegalStateException("You already have an active ride in progress (Ride ID: " + r.getRideId() + ").");
        });

        FareRule fareRule = fareRuleRepository.findByCategoryName(request.getVehicleCategory().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No pricing rules configured for category: " + request.getVehicleCategory()));

        double distanceKm = pricingService.calculateHaversineDistance(
                request.getPickupLat(), request.getPickupLng(),
                request.getDropoffLat(), request.getDropoffLng()
        );
        double surgeMultiplier = pricingService.calculateLiveSurgeMultiplier(
                request.getPickupAddress(), request.getPickupLat(), request.getPickupLng()
        );
        double estimatedFare = pricingService.calculateFareWithRule(distanceKm, surgeMultiplier, fareRule);
        String startPin = String.valueOf(1000 + pinRandom.nextInt(9000));

        Ride ride = Ride.builder()
                .rider(rider)
                .fareRule(fareRule)
                .pickupAddress(request.getPickupAddress().trim())
                .pickupLat(BigDecimal.valueOf(request.getPickupLat()))
                .pickupLng(BigDecimal.valueOf(request.getPickupLng()))
                .dropoffAddress(request.getDropoffAddress().trim())
                .dropoffLat(BigDecimal.valueOf(request.getDropoffLat()))
                .dropoffLng(BigDecimal.valueOf(request.getDropoffLng()))
                .distanceKm(BigDecimal.valueOf(distanceKm))
                .status(RideStatus.PENDING)
                .estimatedFare(BigDecimal.valueOf(estimatedFare))
                .surgeMultiplier(BigDecimal.valueOf(surgeMultiplier))
                .startPin(startPin)
                .requestedAt(LocalDateTime.now())
                .build();

        Ride savedRide = rideRepository.save(ride);

        RideStatusLog initialLog = RideStatusLog.builder()
                .ride(savedRide)
                .fromStatus(RideStatus.PENDING)
                .toStatus(RideStatus.PENDING)
                .changedBy(rider.getUser())
                .changedAt(LocalDateTime.now())
                .reason("Initial ride booking requested")
                .build();
        statusLogRepository.save(initialLog);

        log.info("Ride booked successfully. Ride ID: {}, Fare: Rs. {}", savedRide.getRideId(), estimatedFare);
        return mapToResponse(savedRide, true);
    }

    // ========================================================================
    //  2. View Pending Rides Matching Driver's Vehicle Category
    // ========================================================================
    @Transactional(readOnly = true)
    public List<RideResponse> getAvailablePendingRides(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));

        if (!Boolean.TRUE.equals(driver.getIsAvailable())) {
            return List.of();
        }

        Vehicle vehicle = vehicleRepository.findActiveVehicleByDriver(driver)
                .orElseThrow(() -> new ResourceNotFoundException("No active vehicle found for driver: " + driverId));

        String categoryName = vehicle.getCategory().getCategoryName();
        List<Ride> pendingRides = rideRepository.findPendingRidesByCategory(RideStatus.PENDING, categoryName);
        return pendingRides.stream().map(r -> mapToResponse(r, false)).collect(Collectors.toList());
    }

    // ========================================================================
    //  3. Accept Ride via RAW SQL OPTIMISTIC LOCKING
    // ========================================================================
    @Transactional
    public RideResponse acceptRideOptimistically(Long rideId, Long driverId) {
        log.info("Driver ID: {} attempting to accept Ride ID: {}...", driverId, rideId);

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));

        rideRepository.findActiveRideByDriver(driver).ifPresent(active -> {
            throw new IllegalStateException("You already have an active trip in progress (Ride ID: " + active.getRideId() + ").");
        });

        if (!Boolean.TRUE.equals(driver.getIsAvailable())) {
            throw new IllegalStateException("You are currently Offline. Switch to Online before accepting rides.");
        }

        Vehicle vehicle = vehicleRepository.findActiveVehicleByDriver(driver)
                .orElseThrow(() -> new ResourceNotFoundException("Driver does not have an active vehicle: " + driverId));

        int rowsUpdated = rideRepository.acceptRideOptimisticallyRawSql(
                rideId, driver.getDriverId(), vehicle.getVehicleId()
        );

        if (rowsUpdated == 0) {
            throw new RideAlreadyClaimedException(
                    "This ride has already been accepted by another driver or is no longer available."
            );
        }

        Ride acceptedRide = rideRepository.findById(rideId).orElseThrow();
        driver.setIsAvailable(false);
        driverRepository.save(driver);

        RideStatusLog auditLog = RideStatusLog.builder()
                .ride(acceptedRide)
                .fromStatus(RideStatus.PENDING)
                .toStatus(RideStatus.ACCEPTED)
                .changedBy(driver.getUser())
                .changedAt(LocalDateTime.now())
                .reason("Driver accepted ride")
                .build();
        statusLogRepository.save(auditLog);

        return mapToResponse(acceptedRide, false);
    }

    // ========================================================================
    //  4. Progress Ride Lifecycle (ARRIVED, IN_PROGRESS + PIN Check, CANCELLED)
    // ========================================================================
    @Transactional
    public RideResponse updateRideStatus(Long rideId, Long userId, RideStatus targetStatus, String reason) {
        return updateRideStatus(rideId, userId, targetStatus, null, reason);
    }

    @Transactional
    public RideResponse updateRideStatus(Long rideId, Long userId, RideStatus targetStatus, String startPin, String reason) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        boolean isRider = ride.getRider().getRiderId().equals(userId);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getDriverId().equals(userId);

        if (!isRider && !isDriver) {
            throw new AccessDeniedException("You are not authorized to update this ride.");
        }
        if (targetStatus == RideStatus.COMPLETED) {
            throw new IllegalArgumentException("Use /api/v1/rides/{rideId}/complete to complete a ride and settle payout.");
        }
        if ((targetStatus == RideStatus.ARRIVED || targetStatus == RideStatus.IN_PROGRESS) && !isDriver) {
            throw new AccessDeniedException("Only the assigned driver can transition this ride to " + targetStatus);
        }

        if (targetStatus == RideStatus.IN_PROGRESS && ride.getStartPin() != null) {
            String submittedPin = startPin != null ? startPin.trim() : "";
            if (!ride.getStartPin().equals(submittedPin)) {
                throw new IllegalArgumentException("Invalid 4-digit Ride Start PIN. Ask the passenger for their boarding PIN.");
            }
        }

        User actor = isDriver ? ride.getDriver().getUser() : ride.getRider().getUser();
        stateMachine.transition(ride, targetStatus, actor, reason);

        if (targetStatus == RideStatus.CANCELLED && ride.getDriver() != null) {
            ride.getDriver().setIsAvailable(true);
            driverRepository.save(ride.getDriver());
        }

        Ride updatedRide = rideRepository.save(ride);
        return mapToResponse(updatedRide, isRider);
    }

    // ========================================================================
    //  5. Complete Ride & Trigger @Transactional Settlement
    // ========================================================================
    @Transactional
    public RideResponse completeRide(Long rideId, Long driverId, double collectedFare) {
        settlementService.settleRideCompletion(rideId, driverId, collectedFare);
        Ride completedRide = rideRepository.findById(rideId).orElseThrow();
        return mapToResponse(completedRide, false);
    }

    // ========================================================================
    //  6. Post-Ride Driver Rating (1:1 BCNF RideRating + Avg Update)
    // ========================================================================
    @Transactional
    public RideResponse rateCompletedRide(Long rideId, Long riderId, RateRideRequest request) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (!ride.getRider().getRiderId().equals(riderId)) {
            throw new AccessDeniedException("Only the passenger who booked this ride can submit a rating.");
        }
        if (ride.getStatus() != RideStatus.COMPLETED || ride.getDriver() == null) {
            throw new IllegalStateException("Only completed rides with an assigned driver can be rated.");
        }
        if (rideRatingRepository.existsByRide(ride)) {
            throw new IllegalStateException("This ride has already been rated.");
        }

        RideRating rating = RideRating.builder()
                .ride(ride)
                .stars(request.getStars())
                .comment(request.getComment() != null && !request.getComment().isBlank() ? request.getComment().trim() : null)
                .createdAt(LocalDateTime.now())
                .build();
        rideRatingRepository.save(rating);
        ride.setRating(rating);

        Driver driver = ride.getDriver();
        Double newAvg = rideRatingRepository.calculateAverageStarsByDriverId(driver.getDriverId());
        if (newAvg != null) {
            driver.setAvgRating(BigDecimal.valueOf(newAvg).setScale(2, RoundingMode.HALF_UP));
            driverRepository.save(driver);
        }

        log.info("Ride ID {} rated {} stars by rider ID {}. Driver ID {} avg rating updated to {}",
                rideId, request.getStars(), riderId, driver.getDriverId(), driver.getAvgRating());
        return mapToResponse(ride, true);
    }

    // ========================================================================
    //  7. Audit Timeline & Queries + Pagination
    // ========================================================================
    @Transactional(readOnly = true)
    public List<RideStatusLogResponse> getRideTimeline(Long rideId, Long userId, boolean isAdmin) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        boolean isRider = ride.getRider().getRiderId().equals(userId);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getDriverId().equals(userId);
        if (!isAdmin && !isRider && !isDriver) {
            throw new AccessDeniedException("You do not have permission to view this ride timeline.");
        }

        return statusLogRepository.findByRideOrderByChangedAtAsc(ride).stream()
                .map(entry -> RideStatusLogResponse.builder()
                        .logId(entry.getLogId())
                        .fromStatus(entry.getFromStatus().name())
                        .toStatus(entry.getToStatus().name())
                        .changedBy(entry.getChangedBy().getFullName())
                        .changedByRole(entry.getChangedBy().getRole().name())
                        .changedAt(entry.getChangedAt())
                        .reason(entry.getReason())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RideResponse getRideById(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));
        return mapToResponse(ride, false);
    }

    @Transactional(readOnly = true)
    public RideResponse getRideByIdForUser(Long rideId, Long userId, boolean isAdmin) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        boolean isRider = ride.getRider().getRiderId().equals(userId);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getDriverId().equals(userId);
        if (!isAdmin && !isRider && !isDriver) {
            throw new AccessDeniedException("You do not have permission to view this ride.");
        }
        return mapToResponse(ride, isRider);
    }

    @Transactional(readOnly = true)
    public RideResponse getActiveRideForRider(Long riderId) {
        return riderRepository.findById(riderId)
                .flatMap(rideRepository::findActiveRideByRider)
                .map(r -> mapToResponse(r, true))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public RideResponse getActiveRideForDriver(Long driverId) {
        return driverRepository.findById(driverId)
                .flatMap(rideRepository::findActiveRideByDriver)
                .map(r -> mapToResponse(r, false))
                .orElse(null);
    }

    @Transactional
    public PageResponse<RideResponse> getRiderHistoryPaged(User currentUser, RideStatus status, Pageable pageable) {
        Rider rider = riderRepository.findById(currentUser.getUserId())
                .orElseGet(() -> {
                    User managedUser = userRepository.findById(currentUser.getUserId())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUser.getUserId()));
                    return riderRepository.save(Rider.builder().user(managedUser).isVerified(true).build());
                });

        Page<Ride> page = (status != null)
                ? rideRepository.findByRiderAndStatus(rider, status, pageable)
                : rideRepository.findByRider(rider, pageable);

        List<RideResponse> items = page.getContent().stream()
                .map(r -> mapToResponse(r, true))
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<RideResponse> getDriverHistoryPaged(Long driverId, RideStatus status, Pageable pageable) {
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver == null) {
            return PageResponse.of(Page.empty(pageable), List.of());
        }

        Page<Ride> page = (status != null)
                ? rideRepository.findByDriverAndStatus(driver, status, pageable)
                : rideRepository.findByDriver(driver, pageable);

        List<RideResponse> items = page.getContent().stream()
                .map(r -> mapToResponse(r, false))
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    // ========================================================================
    //  Helper: Entity to DTO Mapper
    // ========================================================================
    private RideResponse mapToResponse(Ride r, boolean includeStartPin) {
        RideRating rating = r.getRating();
        return RideResponse.builder()
                .rideId(r.getRideId())
                .riderId(r.getRider().getRiderId())
                .riderName(r.getRider().getUser().getFullName())
                .driverId(r.getDriver() != null ? r.getDriver().getDriverId() : null)
                .driverName(r.getDriver() != null ? r.getDriver().getUser().getFullName() : null)
                .driverAvgRating(r.getDriver() != null && r.getDriver().getAvgRating() != null
                        ? r.getDriver().getAvgRating().doubleValue() : null)
                .vehicleId(r.getVehicle() != null ? r.getVehicle().getVehicleId() : null)
                .vehicleModel(r.getVehicle() != null ? r.getVehicle().getMakeModel() : null)
                .vehiclePlateNo(r.getVehicle() != null ? r.getVehicle().getPlateNo() : null)
                .vehicleCategory(r.getFareRule().getVehicleCategory().getCategoryName())
                .pickupAddress(r.getPickupAddress())
                .pickupLat(r.getPickupLat() != null ? r.getPickupLat().doubleValue() : null)
                .pickupLng(r.getPickupLng() != null ? r.getPickupLng().doubleValue() : null)
                .dropoffAddress(r.getDropoffAddress())
                .dropoffLat(r.getDropoffLat() != null ? r.getDropoffLat().doubleValue() : null)
                .dropoffLng(r.getDropoffLng() != null ? r.getDropoffLng().doubleValue() : null)
                .distanceKm(r.getDistanceKm() != null ? r.getDistanceKm().doubleValue() : null)
                .status(r.getStatus().name())
                .startPin(includeStartPin ? r.getStartPin() : null)
                .rated(rating != null)
                .ratingStars(rating != null ? rating.getStars() : null)
                .ratingComment(rating != null ? rating.getComment() : null)
                .estimatedFare(r.getEstimatedFare() != null ? r.getEstimatedFare().doubleValue() : null)
                .finalFare(r.getFinalFare() != null ? r.getFinalFare().doubleValue() : null)
                .driverEarning(r.getDriverEarning() != null ? r.getDriverEarning().doubleValue() : null)
                .platformCommission(r.getPlatformCommission() != null ? r.getPlatformCommission().doubleValue() : null)
                .surgeMultiplier(r.getSurgeMultiplier() != null ? r.getSurgeMultiplier().doubleValue() : null)
                .requestedAt(r.getRequestedAt())
                .acceptedAt(r.getAcceptedAt())
                .completedAt(r.getCompletedAt())
                .build();
    }
}
