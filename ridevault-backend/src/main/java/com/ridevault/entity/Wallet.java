package com.ridevault.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity 12/13 — Driver financial account balance.
 *
 * Each driver has exactly one wallet (1:1 relationship).
 * Balance updates are always wrapped in @Transactional with ACID guarantees.
 *
 * Resume: "wallet settlements using Spring Core (DI) and @Transactional
 *          (ACID rollbacks), guaranteeing 100% financial data consistency
 *          during simulated system failures"
 *
 * BCNF: wallet_id → all attributes; driver_id is a candidate key (1:1).
 */
@Entity
@Table(name = "wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Driver driver;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -- Transaction ledger --

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<WalletTransaction> transactions = new ArrayList<>();
}
