package com.ridevault.controller;

import com.ridevault.dto.request.SurgeZoneUpdateRequest;
import com.ridevault.dto.response.ApiResponse;
import com.ridevault.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Role-protected operations for platform metrics, surge zone multipliers, and driver verification")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/overview")
    @Operation(summary = "Platform Overview Metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success("Platform overview retrieved", adminService.getPlatformOverview()));
    }

    @GetMapping("/surge-zones")
    @Operation(summary = "List Surge Zones")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSurgeZones() {
        return ResponseEntity.ok(ApiResponse.success("Surge zones retrieved", adminService.listSurgeZones()));
    }

    @PutMapping("/surge-zones/{surgeId}")
    @Operation(summary = "Update Surge Multiplier")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSurgeMultiplier(
            @PathVariable Long surgeId,
            @Valid @RequestBody SurgeZoneUpdateRequest request) {

        Map<String, Object> updated = adminService.updateSurgeZoneMultiplier(surgeId, request.getMultiplier());
        return ResponseEntity.ok(ApiResponse.success("Surge zone multiplier updated", updated));
    }

    @PutMapping("/drivers/{driverId}/verify")
    @Operation(summary = "Verify or Unverify Driver")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyDriver(
            @PathVariable Long driverId,
            @RequestParam(defaultValue = "true") boolean verified) {

        Map<String, Object> updated = adminService.verifyDriver(driverId, verified);
        return ResponseEntity.ok(ApiResponse.success("Driver verification status updated", updated));
    }
}
