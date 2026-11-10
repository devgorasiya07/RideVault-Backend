package com.ridevault.service;

import com.ridevault.dto.response.PageResponse;
import com.ridevault.dto.response.WalletResponse;
import com.ridevault.entity.*;
import com.ridevault.entity.enums.MovementType;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.exception.InsufficientFundsException;
import com.ridevault.exception.InvalidRideStateException;
import com.ridevault.exception.ResourceNotFoundException;
import com.ridevault.repository.DriverRepository;
import com.ridevault.repository.RideRepository;
import com.ridevault.repository.WalletRepository;
import com.ridevault.repository.WalletTransactionRepository;
import com.ridevault.service.statemachine.RideStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wallet Settlement Service
 *
 * Resume: "wallet settlements using Spring Core (DI) and @Transactional
 *          (ACID rollbacks), guaranteeing 100% financial data consistency
 *          during simulated system failures."
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletSettlementService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final RideStateMachine stateMachine;

    public static final BigDecimal DRIVER_CUT_RATIO = new BigDecimal("0.80");     // 80% to driver
    public static final BigDecimal PLATFORM_COMMISSION_RATIO = new BigDecimal("0.20"); // 20% platform cut

    // ========================================================================
    //  Settle Ride Completion & Wallet Credit (ACID Transaction)
    // ========================================================================
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public WalletTransaction settleRideCompletion(Long rideId, Long driverId, double collectedFare) {
        log.info("Starting ACID settlement for Ride ID: {}, Driver ID: {}, Collected Fare: Rs. {}",
                rideId, driverId, collectedFare);

        BigDecimal fare = BigDecimal.valueOf(collectedFare).setScale(2, RoundingMode.HALF_UP);

        // 1. Fetch Ride
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        // 2. Fetch Driver & verify ownership + ride state before acquiring wallet lock
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));

        if (ride.getDriver() == null || !ride.getDriver().getDriverId().equals(driverId)) {
            throw new AccessDeniedException("Only the assigned driver can complete and settle this ride.");
        }
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStateException(String.format(
                    "Ride [%d] must be IN_PROGRESS to complete settlement (current status: %s)",
                    rideId, ride.getStatus()));
        }

        // Anti-fraud: collectedFare cannot be less than system calculated fare
        if (fare.compareTo(ride.getEstimatedFare()) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Collected fare (Rs. %s) cannot be less than the calculated fare (Rs. %s)",
                    fare, ride.getEstimatedFare()
            ));
        }

        // 3. Calculate 80% driver payout and 20% platform commission
        BigDecimal driverEarning = fare.multiply(DRIVER_CUT_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformCommission = fare.multiply(PLATFORM_COMMISSION_RATIO).setScale(2, RoundingMode.HALF_UP);

        // 4. Lock driver's wallet with Pessimistic Write Lock (single-query)
        Wallet lockedWallet = walletRepository.findByDriverIdWithLock(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for driver ID: " + driverId));

        BigDecimal newBalance = lockedWallet.getBalance().add(driverEarning).setScale(2, RoundingMode.HALF_UP);
        lockedWallet.setBalance(newBalance);
        lockedWallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(lockedWallet);

        // 5. Insert immutable double-entry transaction ledger entry
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(lockedWallet)
                .ride(ride)
                .movementType(MovementType.CREDIT)
                .purpose("Ride Earning (80% cut)")
                .amount(driverEarning)
                .balanceAfter(newBalance)
                .createdAt(LocalDateTime.now())
                .build();
        WalletTransaction savedTxn = transactionRepository.save(transaction);

        // 6. Update Ride details
        ride.setFinalFare(fare);
        ride.setDriverEarning(driverEarning);
        ride.setPlatformCommission(platformCommission);

        // 7. Transition Ride to COMPLETED via State Machine
        stateMachine.transition(ride, RideStatus.COMPLETED, driver.getUser(),
                "Ride successfully completed. Payout settled: Rs. " + driverEarning);
        rideRepository.save(ride);

        // 8. Update driver total rides count and availability
        driver.setTotalRides(driver.getTotalRides() + 1);
        driver.setIsAvailable(true);
        driverRepository.save(driver);

        log.info("ACID Settlement completed successfully. Wallet updated to Rs. {}, Transaction ID: {}",
                newBalance, savedTxn.getTxnId());

        return savedTxn;
    }

    // ========================================================================
    //  Withdraw Funds from Driver Wallet (ACID Transaction)
    // ========================================================================
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public WalletTransaction withdrawFunds(Long driverId, double amount) {
        log.info("Processing withdrawal of Rs. {} for driver ID: {}", amount, driverId);

        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }

        BigDecimal withdrawAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);

        // Single-query pessimistic lock by driver ID
        Wallet lockedWallet = walletRepository.findByDriverIdWithLock(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for driver: " + driverId));

        if (lockedWallet.getBalance().compareTo(withdrawAmount) < 0) {
            throw new InsufficientFundsException(String.format(
                    "Insufficient wallet funds. Requested: Rs. %s, Available: Rs. %s",
                    withdrawAmount, lockedWallet.getBalance()
            ));
        }

        BigDecimal newBalance = lockedWallet.getBalance().subtract(withdrawAmount).setScale(2, RoundingMode.HALF_UP);
        lockedWallet.setBalance(newBalance);
        lockedWallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(lockedWallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(lockedWallet)
                .movementType(MovementType.DEBIT)
                .purpose("Wallet Withdrawal to Bank Account")
                .amount(withdrawAmount)
                .balanceAfter(newBalance)
                .createdAt(LocalDateTime.now())
                .build();

        WalletTransaction savedTxn = transactionRepository.save(txn);
        log.info("Withdrawal completed. New balance: Rs. {}, Txn ID: {}", newBalance, savedTxn.getTxnId());

        return savedTxn;
    }

    // ========================================================================
    //  Get Driver Wallet Information & Recent Ledger History
    // ========================================================================
    @Transactional(readOnly = true)
    public WalletResponse getWalletDetails(Long driverId) {
        Wallet wallet = walletRepository.findByDriverId(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for driver: " + driverId));

        List<WalletTransaction> txns = transactionRepository.findByWalletOrderByCreatedAtDesc(wallet);

        List<WalletResponse.TransactionItem> txnItems = txns.stream()
                .map(this::mapTxnItem)
                .collect(Collectors.toList());

        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .driverId(driverId)
                .driverName(wallet.getDriver().getUser().getFullName())
                .balance(wallet.getBalance().doubleValue())
                .currency(wallet.getCurrency())
                .lastUpdated(wallet.getUpdatedAt())
                .recentTransactions(txnItems)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<WalletResponse.TransactionItem> getWalletTransactionsPaged(
            Long driverId, MovementType movementType, Pageable pageable) {

        Wallet wallet = walletRepository.findByDriverId(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for driver: " + driverId));

        Page<WalletTransaction> page = (movementType != null)
                ? transactionRepository.findByWalletAndMovementType(wallet, movementType, pageable)
                : transactionRepository.findByWallet(wallet, pageable);

        List<WalletResponse.TransactionItem> items = page.getContent().stream()
                .map(this::mapTxnItem)
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    private WalletResponse.TransactionItem mapTxnItem(WalletTransaction t) {
        return WalletResponse.TransactionItem.builder()
                .txnId(t.getTxnId())
                .rideId(t.getRide() != null ? t.getRide().getRideId() : null)
                .movementType(t.getMovementType().name())
                .purpose(t.getPurpose())
                .amount(t.getAmount().doubleValue())
                .balanceAfter(t.getBalanceAfter().doubleValue())
                .createdAt(t.getCreatedAt())
                .build();
    }

    // ========================================================================
    //  SIMULATED SYSTEM FAILURE FOR ACID ROLLBACK VERIFICATION TEST
    //  Resume: "guaranteeing 100% financial data consistency during simulated system failures."
    // ========================================================================
    @Transactional(rollbackFor = Exception.class)
    public void settleRideWithSimulatedFailure(Long rideId, Long driverId, double collectedFare) {
        log.warn("Initiating simulation: Settlement with deliberate unhandled runtime failure...");

        BigDecimal fare = BigDecimal.valueOf(collectedFare);
        BigDecimal driverCut = fare.multiply(DRIVER_CUT_RATIO).setScale(2, RoundingMode.HALF_UP);

        // 1. Credit wallet
        Wallet wallet = walletRepository.findByDriverIdWithLock(driverId).orElseThrow();
        wallet.setBalance(wallet.getBalance().add(driverCut));
        walletRepository.save(wallet);

        // 2. Insert transaction
        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .movementType(MovementType.CREDIT)
                .purpose("Simulated Failure Test")
                .amount(driverCut)
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(txn);

        // 3. Simulate sudden crash / failure mid-settlement
        throw new RuntimeException("SIMULATED_NETWORK_CRASH: Third-party banking gateway connection timed out mid-transaction!");
    }
}
