package com.ridevault.controller;

import com.ridevault.dto.request.*;
import com.ridevault.dto.response.ApiResponse;
import com.ridevault.dto.response.AuthResponse;
import com.ridevault.entity.User;
import com.ridevault.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication & Registration REST Controller.
 * Password-based authentication with BCrypt hashing, stateless JWT access tokens,
 * and stateful Refresh Token Rotation.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for rider/driver registration, BCrypt JWT login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/rider")
    @Operation(summary = "Register a new Rider", description = "Creates a new rider account with BCrypt password hashing and returns JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> registerRider(
            @Valid @RequestBody RiderRegisterRequest request) {

        AuthResponse response = authService.registerRider(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created("Rider registered successfully!", response)
        );
    }

    @PostMapping("/register/driver")
    @Operation(summary = "Register a new Driver", description = "Creates driver, vehicle, insurance document, and initializes wallet")
    public ResponseEntity<ApiResponse<AuthResponse>> registerDriver(
            @Valid @RequestBody DriverRegisterRequest request) {

        AuthResponse response = authService.registerDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created("Driver registered successfully with vehicle and wallet initialized!", response)
        );
    }

    @PostMapping("/login")
    @Operation(summary = "User Login (Email + Password)", description = "Authenticates rider, driver, or admin via BCrypt and issues JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Authentication successful!", response)
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Rotates a valid refresh token and issues a new JWT access + refresh token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(ApiResponse.success("Access token refreshed successfully!", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout & Revoke Tokens", description = "Revokes all active refresh tokens for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User currentUser) {

        authService.logout(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Logged out and refresh tokens revoked.", null));
    }
}
