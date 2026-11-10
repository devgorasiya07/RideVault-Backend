package com.ridevault.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity 14/14 — Post-ride driver feedback and star rating.
 *
 * Strictly BCNF: rating_id -> all attributes; ride_id is a candidate key (1:1).
 * Rider and driver identities are derived through ride_id without storing
 * redundant transitive foreign keys.
 */
@Entity
@Table(name = "ride_ratings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long ratingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Ride ride;

    @Column(nullable = false)
    private Integer stars;

    @Column(length = 255)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
