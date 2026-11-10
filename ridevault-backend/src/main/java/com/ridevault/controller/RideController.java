package com.ridevault.controller;

import com.ridevault.dto.request.RateRideRequest;
import com.ridevault.dto.request.RideBookingRequest;
import com.ridevault.dto.response.ApiResponse;
import com.ridevault.dto.response.FareEstimateResponse;
import com.ridevault.dto.response.RideResponse;
import com.ridevault.dto.response.RideStatusLogResponse;
import com.ridevault.entity.User;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.entity.enums.Role;
import com.ridevault.service.PricingService;
import com.ridevault.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ride Management REST Controller
 */
@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
@Tag(name = "Rides", description = "Endpoints for ride booking, matching, optimistic acceptance, start PIN verification, ratings, and timeline")
@SecurityRequirement(name = "Bearer Authentication")
public class RideController {

    private final RideService rideService;
    private final PricingService pricingService;

    @GetMapping("/estimate-fares")
    @Operation(summary = "Get Fare Estimates", description = "Calculates Haversine distance and estimated fares across all categories including live surge")
    public ResponseEntity<ApiResponse<FareEstimateResponse>> estimateFares(
            @RequestParam String pickupAddress,
            @RequestParam double pickupLat,
            @RequestParam double pickupLng,
            @RequestParam String dropoffAddress,
            @RequestParam double dropoffLat,
            @RequestParam double dropoffLng) {

        FareEstimateResponse response = pricingService.estimateFares(
                pickupAddress, pickupLat, pickupLng, dropoffAddress, dropoffLat, dropoffLng
        );
        return ResponseEntity.ok(ApiResponse.success("Fare estimates calculated", response));
    }

    @PostMapping("/book")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Book a New Ride", description = "Creates a new ride in PENDING state and issues a 4-digit Start PIN for the rider")
    public ResponseEntity<ApiResponse<RideResponse>> bookRide(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RideBookingRequest request) {

        RideResponse response = rideService.bookRide(currentUser.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created("Ride booked successfully! Share your Start PIN when the driver arrives.", response)
        );
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "View Pending Rides", description = "Returns all pending rides matching driver's vehicle category when driver is Online")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getAvailableRides(
            @AuthenticationPrincipal User currentUser) {

        List<RideResponse> rides = rideService.getAvailablePendingRides(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Available pending rides retrieved", rides));
    }

    @PutMapping("/{rideId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Accept Ride (Optimistic Locking)",
               description = "Claims a pending ride atomically using WHERE status = 'PENDING'")
    public ResponseEntity<ApiResponse<RideResponse>> acceptRide(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long rideId) {

        RideResponse response = rideService.acceptRideOptimistically(rideId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Ride accepted successfully!", response));
    }

    @PutMapping("/{rideId}/status")
    @Operation(summary = "Update Ride Status", description = "Transitions ride along the state machine (ARRIVED, IN_PROGRESS with 4-digit startPin, CANCELLED)")
    public ResponseEntity<ApiResponse<RideResponse>> updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long rideId,
            @RequestParam RideStatus targetStatus,
            @RequestParam(required = false) String startPin,
            @RequestParam(required = false, defaultValue = "Status update requested") String reason) {

        RideResponse response = rideService.updateRideStatus(
                rideId, currentUser.getUserId(), targetStatus, startPin, reason
        );
        return ResponseEntity.ok(ApiResponse.success("Ride status updated to " + targetStatus, response));
    }

    @PutMapping("/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Complete Ride & Settle Payout",
               description = "Triggers @Transactional ACID settlement, crediting 80% to driver wallet and 20% commission")
    public ResponseEntity<ApiResponse<RideResponse>> completeRide(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long rideId,
            @RequestParam double collectedFare) {

        RideResponse response = rideService.completeRide(rideId, currentUser.getUserId(), collectedFare);
        return ResponseEntity.ok(ApiResponse.success("Ride completed and payout credited to wallet!", response));
    }

    @PostMapping("/{rideId}/rate")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Rate Completed Ride", description = "Submits a 1-5 star rating for the driver and recalculates driver average rating")
    public ResponseEntity<ApiResponse<RideResponse>> rateRide(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long rideId,
            @Valid @RequestBody RateRideRequest request) {

        RideResponse response = rideService.rateCompletedRide(rideId, currentUser.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created("Thank you! Driver rating recorded.", response)
        );
    }

    @GetMapping("/{rideId}/timeline")
    @Operation(summary = "Get Ride Audit Timeline", description = "Returns chronological ride_status_logs entries for a ride")
    public ResponseEntity<ApiResponse<List<RideStatusLogResponse>>> getRideTimeline(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long rideId) {

        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
        List<RideStatusLogResponse> timeline = rideService.getRideTimeline(rideId, currentUser.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Ride status timeline retrieved", timeline));
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get Ride Details", description = "Fetches complete details and lifecycle status of a ride")
    public ResponseEntity<ApiResponse<RideResponse>> getRideById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long rideId) {
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
        RideResponse response = rideService.getRideByIdForUser(rideId, currentUser.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Ride details retrieved", response));
    }
}
