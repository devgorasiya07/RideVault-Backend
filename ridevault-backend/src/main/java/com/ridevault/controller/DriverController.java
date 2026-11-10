package com.ridevault.controller;

import com.ridevault.dto.response.ApiResponse;
import com.ridevault.dto.response.PageResponse;
import com.ridevault.dto.response.RideResponse;
import com.ridevault.entity.Driver;
import com.ridevault.entity.Ride;
import com.ridevault.entity.User;
import com.ridevault.entity.Vehicle;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.exception.ResourceNotFoundException;
import com.ridevault.repository.DriverRepository;
import com.ridevault.repository.RideRepository;
import com.ridevault.repository.VehicleRepository;
import com.ridevault.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
@Tag(name = "Driver", description = "Endpoints for driver profile, online/offline availability, vehicle status, and paginated history")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('DRIVER')")
public class DriverController {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    private final RideService rideService;

    @GetMapping("/active-ride")
    @Operation(summary = "Get Driver's Active Trip", description = "Retrieves current ongoing trip (ACCEPTED, ARRIVED, IN_PROGRESS) for logged-in driver")
    public ResponseEntity<ApiResponse<RideResponse>> getActiveRide(
            @AuthenticationPrincipal User currentUser) {

        RideResponse activeRide = rideService.getActiveRideForDriver(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(
                activeRide != null ? "Active trip found" : "No active trip in progress", activeRide
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Driver Profile & Active Vehicle Details")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal User currentUser) {

        Driver driver = driverRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        Vehicle vehicle = vehicleRepository.findActiveVehicleByDriver(driver).orElse(null);

        Map<String, Object> profile = new HashMap<>();
        profile.put("driverId", driver.getDriverId());
        profile.put("fullName", currentUser.getFullName());
        profile.put("email", currentUser.getEmail());
        profile.put("phoneNo", currentUser.getPhoneNo());
        profile.put("licenseNo", driver.getLicenseNo());
        profile.put("rating", driver.getAvgRating());
        profile.put("totalRides", driver.getTotalRides());
        profile.put("isVerified", driver.getIsVerified());
        profile.put("isAvailable", driver.getIsAvailable());

        if (vehicle != null) {
            Map<String, Object> vMap = new HashMap<>();
            vMap.put("vehicleId", vehicle.getVehicleId());
            vMap.put("model", vehicle.getMakeModel());
            vMap.put("year", vehicle.getManufactureYear());
            vMap.put("plateNo", vehicle.getPlateNo());
            vMap.put("category", vehicle.getCategory().getCategoryName());
            profile.put("vehicle", vMap);
        }

        return ResponseEntity.ok(ApiResponse.success("Driver profile retrieved", profile));
    }

    @PutMapping("/availability")
    @Operation(summary = "Toggle Driver Online / Offline Status")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAvailability(
            @AuthenticationPrincipal User currentUser,
            @RequestParam boolean available) {

        Driver driver = driverRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (!available && rideRepository.findActiveRideByDriver(driver).isPresent()) {
            throw new IllegalStateException("Cannot go Offline while you have an active ride in progress.");
        }

        driver.setIsAvailable(available);
        Driver saved = driverRepository.save(driver);

        Map<String, Object> res = new HashMap<>();
        res.put("driverId", saved.getDriverId());
        res.put("isAvailable", saved.getIsAvailable());

        return ResponseEntity.ok(ApiResponse.success(
                available ? "You are now ONLINE and ready to receive rides." : "You are now OFFLINE.", res
        ));
    }

    @GetMapping("/rides")
    @Operation(summary = "Get Driver Trip History")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRideHistory(
            @AuthenticationPrincipal User currentUser) {

        Driver driver = driverRepository.findById(currentUser.getUserId()).orElse(null);
        if (driver == null) {
            return ResponseEntity.ok(ApiResponse.success("Driver profile not found", List.of()));
        }
        List<Ride> rides = rideRepository.findByDriverOrderByRequestedAtDesc(driver);

        List<Map<String, Object>> list = rides.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("rideId", r.getRideId());
            map.put("riderName", r.getRider().getUser().getFullName());
            map.put("pickup", r.getPickupAddress());
            map.put("dropoff", r.getDropoffAddress());
            map.put("fare", r.getFinalFare() != null ? r.getFinalFare() : r.getEstimatedFare());
            map.put("earning", r.getDriverEarning());
            map.put("status", r.getStatus().name());
            map.put("requestedAt", r.getRequestedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Driver history retrieved", list));
    }

    @GetMapping("/rides/paged")
    @Operation(summary = "Get Paginated Driver Trip History", description = "Supports pagination and optional status filtering")
    public ResponseEntity<ApiResponse<PageResponse<RideResponse>>> getRideHistoryPaged(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) RideStatus status) {

        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "requestedAt"));
        PageResponse<RideResponse> paged = rideService.getDriverHistoryPaged(currentUser.getUserId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Paginated driver trip history retrieved", paged));
    }
}
