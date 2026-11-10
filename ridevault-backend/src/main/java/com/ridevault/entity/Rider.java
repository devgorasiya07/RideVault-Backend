package com.ridevault.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity 2/13 — Rider-specific profile metadata.
 *
 * Shares primary key with users table via @MapsId (shared PK pattern).
 * BCNF: rider_id → all rider-specific attributes.
 */
@Entity
@Table(name = "riders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rider {

    @Id
    @Column(name = "rider_id")
    private Long riderId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "rider_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "emergency_contact", length = 15)
    private String emergencyContact;
}
