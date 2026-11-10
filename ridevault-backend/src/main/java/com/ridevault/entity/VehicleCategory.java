package com.ridevault.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity 5/13 — Master catalog of vehicle classes.
 *
 * Separated from fare_rules to eliminate transitive dependency
 * (vehicle_category string → capacity/description).
 *
 * BCNF: category_id → all attributes; category_name is a candidate key.
 */
@Entity
@Table(name = "vehicle_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", nullable = false, unique = true, length = 20)
    private String categoryName;

    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 4;

    @Column(length = 200)
    private String description;
}
