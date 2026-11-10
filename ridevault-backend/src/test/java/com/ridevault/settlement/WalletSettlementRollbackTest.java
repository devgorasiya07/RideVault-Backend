package com.ridevault.settlement;

import com.ridevault.entity.*;
import com.ridevault.entity.enums.Role;
import com.ridevault.repository.DriverRepository;
import com.ridevault.repository.UserRepository;
import com.ridevault.repository.WalletRepository;
import com.ridevault.repository.WalletTransactionRepository;
import com.ridevault.service.WalletSettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ACID Rollback Verification Test
 *
 * Resume: "wallet settlements using Spring Core (DI) and @Transactional (ACID rollbacks),
 *          guaranteeing 100% financial data consistency during simulated system failures."
 */
@SpringBootTest
public class WalletSettlementRollbackTest {

    @Autowired
    private WalletSettlementService walletSettlementService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify 100% financial data consistency & zero balance drift during simulated mid-settlement failure")
    void testWalletRollbackOnSimulatedFailure() {
        // 1. Arrange: Create test user, driver, and initial wallet
        String testEmail = "test_driver_" + System.currentTimeMillis() + "@ridevault.com";
        User user = User.builder()
                .firstName("Arjun")
                .lastName("Patel")
                .email(testEmail)
                .phoneNo("98" + (System.currentTimeMillis() % 100000000))
                .passwordHash("$2a$10$hashedpassword")
                .role(Role.ROLE_DRIVER)
                .build();
        user = userRepository.save(user);

        Driver driver = Driver.builder()
                .user(user)
                .licenseNo("GJ01" + (System.currentTimeMillis() % 1000000))
                .build();
        driver = driverRepository.save(driver);

        final BigDecimal initialBalance = new BigDecimal("1500.00");
        Wallet wallet = Wallet.builder()
                .driver(driver)
                .balance(initialBalance)
                .currency("INR")
                .build();
        wallet = walletRepository.save(wallet);

        long initialTxnCount = transactionRepository.findByWalletOrderByCreatedAtDesc(wallet).size();

        // 2. Act: Trigger settlement with simulated unhandled runtime crash
        final Long driverId = driver.getDriverId();
        final double collectedFare = 500.00; // 80% cut would be 400.00

        assertThrows(RuntimeException.class, () -> {
            walletSettlementService.settleRideWithSimulatedFailure(1L, driverId, collectedFare);
        }, "Should throw simulated runtime crash");

        // 3. Assert: Verify ACID rollback guaranteed 100% data consistency
        Wallet walletAfterCrash = walletRepository.findByDriverId(driverId).orElseThrow();
        long txnCountAfterCrash = transactionRepository.findByWalletOrderByCreatedAtDesc(walletAfterCrash).size();

        // Balance MUST remain exactly 1500.00 (Zero financial drift)
        assertEquals(0, initialBalance.compareTo(walletAfterCrash.getBalance()),
                "ACID Rollback Failed! Balance was altered during crash. Expected: "
                        + initialBalance + ", Got: " + walletAfterCrash.getBalance());

        // Transaction record MUST NOT have been committed
        assertEquals(initialTxnCount, txnCountAfterCrash,
                "ACID Rollback Failed! Transaction row was committed despite failure.");
    }
}
