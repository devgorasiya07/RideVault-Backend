package com.ridevault.concurrency;

import com.ridevault.entity.*;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.entity.enums.Role;
import com.ridevault.exception.RideAlreadyClaimedException;
import com.ridevault.repository.*;
import com.ridevault.service.RideService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * High-Concurrency Optimistic Locking Test
 *
 * Resume: "executing high-concurrency updates via raw SQL optimistic locking
 *          (WHERE status='PENDING') to eradicate double-booking race conditions under load."
 */
@SpringBootTest
public class RideConcurrencyTest {

    @Autowired
    private RideService rideService;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Autowired
    private FareRuleRepository fareRuleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify race condition eradication: 10 concurrent drivers racing for 1 pending ride yields exactly 1 winner and 9 rejections")
    void testOptimisticLockingPreventsDoubleBooking() throws InterruptedException {
        // 1. Arrange: Create 1 rider and 1 pending ride
        String riderEmail = "concurrency_rider_" + System.currentTimeMillis() + "@ridevault.com";
        User riderUser = userRepository.save(User.builder()
                .firstName("Karan").lastName("Shah")
                .email(riderEmail).phoneNo(String.format("91%08d", System.currentTimeMillis() % 100000000))
                .passwordHash("hash").role(Role.ROLE_RIDER).build());
        Rider rider = riderRepository.save(Rider.builder().user(riderUser).build());

        VehicleCategory category = categoryRepository.findByCategoryNameIgnoreCase("SEDAN")
                .orElseGet(() -> categoryRepository.save(VehicleCategory.builder()
                        .categoryName("SEDAN").capacity(4).build()));

        FareRule fareRule = fareRuleRepository.findByCategoryName("SEDAN")
                .orElseGet(() -> fareRuleRepository.save(FareRule.builder()
                        .vehicleCategory(category)
                        .baseFare(new BigDecimal("50.00"))
                        .perKmRate(new BigDecimal("12.00"))
                        .perMinRate(new BigDecimal("1.50"))
                        .minFare(new BigDecimal("80.00"))
                        .build()));

        Ride ride = rideRepository.save(Ride.builder()
                .rider(rider).fareRule(fareRule)
                .pickupAddress("Infocity")
                .pickupLat(new BigDecimal("23.1885"))
                .pickupLng(new BigDecimal("72.6285"))
                .dropoffAddress("GIFT City")
                .dropoffLat(new BigDecimal("23.1610"))
                .dropoffLng(new BigDecimal("72.6823"))
                .estimatedFare(new BigDecimal("250.00"))
                .status(RideStatus.PENDING).build());

        final Long rideId = ride.getRideId();

        // 2. Arrange: Create 10 different verified drivers, each with their own vehicle
        final int THREAD_COUNT = 10;
        List<Driver> drivers = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            User driverUser = userRepository.save(User.builder()
                    .firstName("Driver" + i).lastName("Singh")
                    .email("driver_thread_" + i + "_" + System.currentTimeMillis() + "@ridevault.com")
                    .phoneNo(String.format("97%08d", (System.currentTimeMillis() + i * 1000L) % 100000000))
                    .passwordHash("hash").role(Role.ROLE_DRIVER).build());

            Driver driver = driverRepository.save(Driver.builder()
                    .user(driverUser).licenseNo("DL" + i + "_" + System.currentTimeMillis()).build());

            vehicleRepository.save(Vehicle.builder()
                    .driver(driver).category(category).makeModel("Dzire")
                    .manufactureYear(2022).plateNo("GJ01" + i + (System.currentTimeMillis() % 10000))
                    .isActive(true).build());

            drivers.add(driver);
        }

        // 3. Act: Launch all 10 drivers simultaneously using CountDownLatch
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (Driver d : drivers) {
            executor.submit(() -> {
                try {
                    startGun.await(); // Wait for simultaneous firing of all threads
                    rideService.acceptRideOptimistically(rideId, d.getDriverId());
                    successCount.incrementAndGet();
                } catch (RideAlreadyClaimedException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    unexpectedErrorCount.incrementAndGet();
                    ex.printStackTrace();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        // Fire the gun!
        startGun.countDown();
        assertTrue(finishLine.await(10, TimeUnit.SECONDS), "Concurrent threads timed out");
        executor.shutdown();

        // 4. Assert: Exactly 1 driver must have claimed the ride; 9 must have been safely rejected
        assertEquals(0, unexpectedErrorCount.get(), "No unexpected exceptions should occur");
        assertEquals(1, successCount.get(), "Exactly ONE driver should succeed in claiming the pending ride");
        assertEquals(9, conflictCount.get(), "Exactly NINE drivers should receive RideAlreadyClaimedException");

        // Verify database state: status must be ACCEPTED, exactly 1 driver assigned
        Ride finalRide = rideRepository.findById(rideId).orElseThrow();
        assertEquals(RideStatus.ACCEPTED, finalRide.getStatus());
        assertNotNull(finalRide.getDriver(), "Driver must be assigned");
    }
}
