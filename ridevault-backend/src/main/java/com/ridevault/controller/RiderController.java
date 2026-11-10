package com.ridevault.controller;

import com.ridevault.dto.request.UpdateRiderProfileRequest;
import com.ridevault.dto.response.ApiResponse;
import com.ridevault.dto.response.PageResponse;
import com.ridevault.dto.response.RideResponse;
import com.ridevault.entity.Ride;
import com.ridevault.entity.Rider;
import com.ridevault.entity.User;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.exception.ResourceNotFoundException;
import com.ridevault.repository.RideRepository;
import com.ridevault.repository.RiderRepository;
import com.ridevault.repository.UserRepository;
import com.ridevault.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rider")
@RequiredArgsConstructor
@Tag(name = "Rider", description = "Endpoints for rider profile, emergency contact, active ride, and paginated history")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('RIDER')")
public class RiderController {

    private final RiderRepository riderRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RideService rideService;

    @GetMapping("/active-ride")
    @Operation(summary = "Get Current Active Ride", description = "Retrieves ongoing ride (PENDING, ACCEPTED, ARRIVED, IN_PROGRESS) for current rider")
    public ResponseEntity<ApiResponse<RideResponse>> getActiveRide(
            @AuthenticationPrincipal User currentUser) {

        RideResponse activeRide = rideService.getActiveRideForRider(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(
                activeRide != null ? "Active ride found" : "No active ride in progress", activeRide
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Rider Profile")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal User currentUser) {

        Rider rider = findOrCreateManagedRider(currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", buildProfileMap(currentUser, rider)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update Rider Emergency Contact")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateRiderProfileRequest request) {

        Rider rider = findOrCreateManagedRider(currentUser.getUserId());

        String contact = request.getEmergencyContact() != null ? request.getEmergencyContact().trim() : null;
        rider.setEmergencyContact(contact != null && !contact.isEmpty() ? contact : null);
        Rider saved = riderRepository.save(rider);

        return ResponseEntity.ok(ApiResponse.success("Emergency contact updated!", buildProfileMap(currentUser, saved)));
    }

    @GetMapping("/rides")
    @Operation(summary = "Get Rider Trip History")
    @Transactional
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRideHistory(
            @AuthenticationPrincipal User currentUser) {

        Rider rider = findOrCreateManagedRider(currentUser.getUserId());
        List<Ride> rides = rideRepository.findByRiderOrderByRequestedAtDesc(rider);

        List<Map<String, Object>> list = rides.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("rideId", r.getRideId());
            map.put("pickup", r.getPickupAddress());
            map.put("dropoff", r.getDropoffAddress());
            map.put("fare", r.getFinalFare() != null ? r.getFinalFare() : r.getEstimatedFare());
            map.put("status", r.getStatus().name());
            map.put("requestedAt", r.getRequestedAt());
            map.put("rated", r.getRating() != null);
            map.put("ratingStars", r.getRating() != null ? r.getRating().getStars() : null);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Ride history retrieved", list));
    }

    @GetMapping("/rides/paged")
    @Operation(summary = "Get Paginated Rider Trip History", description = "Supports pagination and optional status filtering")
    public ResponseEntity<ApiResponse<PageResponse<RideResponse>>> getRideHistoryPaged(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) RideStatus status) {

        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "requestedAt"));
        PageResponse<RideResponse> paged = rideService.getRiderHistoryPaged(currentUser, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Paginated ride history retrieved", paged));
    }

    private Rider findOrCreateManagedRider(Long userId) {
        return riderRepository.findById(userId)
                .orElseGet(() -> {
                    User managedUser = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    return riderRepository.save(Rider.builder().user(managedUser).isVerified(true).build());
                });
    }

    private Map<String, Object> buildProfileMap(User currentUser, Rider rider) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("riderId", rider.getRiderId());
        profile.put("fullName", currentUser.getFullName());
        profile.put("email", currentUser.getEmail());
        profile.put("phoneNo", currentUser.getPhoneNo());
        profile.put("isVerified", rider.getIsVerified());
        profile.put("emergencyContact", rider.getEmergencyContact());
        profile.put("memberSince", currentUser.getCreatedAt());
        return profile;
    }
}
