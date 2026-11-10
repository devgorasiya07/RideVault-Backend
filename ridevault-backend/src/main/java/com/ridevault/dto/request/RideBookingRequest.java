package com.ridevault.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ride booking request DTO with location and vehicle category validation.
 *
 * Resume: "@Valid DTOs, streamlining safe client integration and rejecting
 *          malformed API requests"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideBookingRequest {

    @NotBlank(message = "Pickup location name is required")
    private String pickupAddress;

    @NotNull(message = "Pickup latitude is required")
    private Double pickupLat;

    @NotNull(message = "Pickup longitude is required")
    private Double pickupLng;

    @NotBlank(message = "Drop-off location name is required")
    private String dropoffAddress;

    @NotNull(message = "Drop-off latitude is required")
    private Double dropoffLat;

    @NotNull(message = "Drop-off longitude is required")
    private Double dropoffLng;

    @NotBlank(message = "Vehicle category is required")
    @Pattern(regexp = "^(MINI|SEDAN|SUV|PREMIUM|AUTO|BIKE)$",
             message = "Vehicle category must be one of: MINI, SEDAN, SUV, PREMIUM, AUTO, BIKE")
    private String vehicleCategory;
}
