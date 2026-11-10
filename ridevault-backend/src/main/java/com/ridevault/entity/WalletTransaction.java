package com.ridevault.entity;

import com.ridevault.entity.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity 13/13 — Double-entry financial ledger.
 *
 * Every wallet balance change (ride earning credit, withdrawal debit)
 * is recorded as an immutable transaction row.
 *
 * Resume: "wallet settlements using Spring Core (DI) and @Transactional
 *          (ACID rollbacks), guaranteeing 100% financial data consistency
 *          during simulated system failures"
 *
 * BCNF: txn_id → all attributes.
 */
@Entity
@Table(name = "wallet_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Long txnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Ride ride;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType movementType;

    @Column(nullable = false, length = 100)
    private String purpose;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
