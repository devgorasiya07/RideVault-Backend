package com.ridevault.controller;

import com.ridevault.dto.response.ApiResponse;
import com.ridevault.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/hub")
@RequiredArgsConstructor
@Tag(name = "Operations Hub", description = "Public read-only aggregated PostgreSQL metrics and surge status for the System Hub dashboard")
public class HubController {

    private final AdminService adminService;

    @GetMapping("/overview")
    @Operation(summary = "Get Live Platform Telemetry", description = "Returns aggregated ride counts, online drivers, commission revenue, and active surge zones from PostgreSQL")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLiveTelemetry() {
        return ResponseEntity.ok(ApiResponse.success("Live platform metrics retrieved", adminService.getPlatformOverview()));
    }
}
