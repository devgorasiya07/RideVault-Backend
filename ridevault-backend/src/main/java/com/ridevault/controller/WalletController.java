package com.ridevault.controller;

import com.ridevault.dto.request.WithdrawRequest;
import com.ridevault.dto.response.ApiResponse;
import com.ridevault.dto.response.PageResponse;
import com.ridevault.dto.response.WalletResponse;
import com.ridevault.entity.User;
import com.ridevault.entity.WalletTransaction;
import com.ridevault.entity.enums.MovementType;
import com.ridevault.service.WalletSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Driver Wallet REST Controller
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Endpoints for driver wallet balance tracking, paginated ledger, and withdrawal settlements")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('DRIVER')")
public class WalletController {

    private final WalletSettlementService walletService;

    @GetMapping("/me")
    @Operation(summary = "Get Wallet Balance & Transactions",
               description = "Returns the driver's current wallet balance and immutable transaction ledger history")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal User currentUser) {

        WalletResponse wallet = walletService.getWalletDetails(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Wallet details retrieved successfully", wallet));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get Paginated Wallet Transactions",
               description = "Supports pagination and optional CREDIT/DEBIT movementType filter")
    public ResponseEntity<ApiResponse<PageResponse<WalletResponse.TransactionItem>>> getTransactionsPaged(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) MovementType movementType) {

        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<WalletResponse.TransactionItem> result = walletService.getWalletTransactionsPaged(
                currentUser.getUserId(), movementType, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Paginated wallet ledger retrieved", result));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw Wallet Funds",
               description = "Atomically debits funds from the driver's wallet with ACID rollback protection")
    public ResponseEntity<ApiResponse<WalletResponse.TransactionItem>> withdrawFunds(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody WithdrawRequest request) {

        WalletTransaction txn = walletService.withdrawFunds(currentUser.getUserId(), request.getAmount());

        WalletResponse.TransactionItem item = WalletResponse.TransactionItem.builder()
                .txnId(txn.getTxnId())
                .movementType(txn.getMovementType().name())
                .purpose(txn.getPurpose())
                .amount(txn.getAmount().doubleValue())
                .balanceAfter(txn.getBalanceAfter().doubleValue())
                .createdAt(txn.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Withdrawal processed successfully!", item));
    }
}
