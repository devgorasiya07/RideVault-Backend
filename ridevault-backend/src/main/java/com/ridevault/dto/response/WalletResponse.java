package com.ridevault.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wallet balance and recent transactions response DTO.
 *
 * Resume: "wallet settlements using Spring Core (DI) and @Transactional
 *          (ACID rollbacks)"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletResponse {

    private Long walletId;
    private Long driverId;
    private String driverName;
    private Double balance;
    private String currency;
    private LocalDateTime lastUpdated;
    private List<TransactionItem> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionItem {
        private Long txnId;
        private Long rideId;
        private String movementType;
        private String purpose;
        private Double amount;
        private Double balanceAfter;
        private LocalDateTime createdAt;
    }
}
