package com.ridevault.entity;

import com.ridevault.entity.enums.RideStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity 10/13 — Central trip ledger.
 *
 * This entity is the target of optimistic locking for concurrent ride acceptance:
 *   UPDATE rides SET ... WHERE ride_id = ? AND status = 'PENDING'
 *
 * Resume: "executing high-concurrency updates via raw SQL optimistic locking
 *          (WHERE status='PENDING') to eradicate double-booking race conditions
 *          under load"
 *
 * BCNF: ride_id → all attributes.
 */
@Entity
@Table(name = "rides")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ride_id")
    private Long rideId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Rider rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FareRule fareRule;

    // -- Route --

    @Column(name = "pickup_address", nullable = false, length = 200)
    private String pickupAddress;

    @Column(name = "pickup_lat", nullable = false, precision = 10, scale = 6)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", nullable = false, precision = 10, scale = 6)
    private BigDecimal pickupLng;

    @Column(name = "dropoff_address", nullable = false, length = 200)
    private String dropoffAddress;

    @Column(name = "dropoff_lat", nullable = false, precision = 10, scale = 6)
    private BigDecimal dropoffLat;

    @Column(name = "dropoff_lng", nullable = false, precision = 10, scale = 6)
    private BigDecimal dropoffLng;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    // -- State --

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RideStatus status = RideStatus.PENDING;

    // -- Fare --

    @Column(name = "estimated_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedFare;

    @Column(name = "final_fare", precision = 10, scale = 2)
    private BigDecimal finalFare;

    @Column(name = "driver_earning", precision = 10, scale = 2)
    private BigDecimal driverEarning;

    @Column(name = "platform_commission", precision = 10, scale = 2)
    private BigDecimal platformCommission;

    @Column(name = "surge_multiplier", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal surgeMultiplier = BigDecimal.ONE;

    @Column(name = "start_pin", length = 4)
    private String startPin;

    // -- Timestamps --

    @Column(name = "requested_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // -- Audit trail & Rating --

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<RideStatusLog> statusLogs = new ArrayList<>();

    @OneToOne(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RideRating rating;
}
