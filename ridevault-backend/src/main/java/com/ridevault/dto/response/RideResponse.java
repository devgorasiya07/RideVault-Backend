package com.ridevault.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ride details response DTO returned from ride endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideResponse {

    private Long rideId;
    private Long riderId;
    private String riderName;
    private Long driverId;
    private String driverName;
    private Double driverAvgRating;
    private Long vehicleId;
    private String vehicleModel;
    private String vehiclePlateNo;
    private String vehicleCategory;

    // Route
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private String dropoffAddress;
    private Double dropoffLat;
    private Double dropoffLng;
    private Double distanceKm;

    // Fare
    private Double estimatedFare;
    private Double finalFare;
    private Double driverEarning;
    private Double platformCommission;
    private Double surgeMultiplier;

    // State & Boarding Verification
    private String status;
    private String startPin;
    private Boolean rated;
    private Integer ratingStars;
    private String ratingComment;
    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
