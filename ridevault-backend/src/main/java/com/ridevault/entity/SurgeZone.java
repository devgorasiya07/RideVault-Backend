package com.ridevault.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity 9/13 — Geographic surge pricing zone.
 *
 * Defines circular zones around Gandhinagar landmarks where dynamic
 * surge multipliers may apply during peak demand.
 *
 * BCNF: surge_id → all attributes; zone_name is a candidate key.
 */
@Entity
@Table(name = "surge_zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurgeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "surge_id")
    private Long surgeId;

    @Column(name = "zone_name", nullable = false, unique = true, length = 100)
    private String zoneName;

    @Column(name = "center_lat", nullable = false, precision = 10, scale = 6)
    private BigDecimal centerLat;

    @Column(name = "center_lng", nullable = false, precision = 10, scale = 6)
    private BigDecimal centerLng;

    @Column(name = "radius_km", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal radiusKm = new BigDecimal("5.00");

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal multiplier = BigDecimal.ONE;

    @Column(name = "active_from")
    private LocalDateTime activeFrom;

    @Column(name = "active_until")
    private LocalDateTime activeUntil;
}
