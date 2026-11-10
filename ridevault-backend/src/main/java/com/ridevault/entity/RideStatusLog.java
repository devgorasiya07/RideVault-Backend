package com.ridevault.entity;

import com.ridevault.entity.enums.RideStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity 11/13 — Immutable audit trail of ride state transitions.
 *
 * Every state change in the ride lifecycle is logged here as an
 * append-only record. This supports debugging, dispute resolution,
 * and demonstrates the state machine pattern.
 *
 * Resume: "Orchestrated ride state machine workflows"
 *
 * BCNF: log_id → all attributes.
 */
@Entity
@Table(name = "ride_status_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Ride ride;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private RideStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private RideStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(length = 255)
    private String reason;
}
