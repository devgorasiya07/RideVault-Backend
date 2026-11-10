package com.ridevault.repository;

import com.ridevault.entity.Driver;
import com.ridevault.entity.Ride;
import com.ridevault.entity.Rider;
import com.ridevault.entity.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE rides SET driver_id = :driverId, vehicle_id = :vehicleId, " +
                   "status = 'ACCEPTED', accepted_at = CURRENT_TIMESTAMP " +
                   "WHERE ride_id = :rideId AND status = 'PENDING'", nativeQuery = true)
    int acceptRideOptimisticallyRawSql(@Param("rideId") Long rideId,
                                       @Param("driverId") Long driverId,
                                       @Param("vehicleId") Long vehicleId);

    @Query("SELECT r FROM Ride r " +
           "JOIN FETCH r.rider rd " +
           "JOIN FETCH rd.user " +
           "JOIN FETCH r.fareRule f " +
           "JOIN FETCH f.vehicleCategory c " +
           "WHERE r.status = :status AND UPPER(c.categoryName) = UPPER(:categoryName) " +
           "ORDER BY r.requestedAt ASC")
    List<Ride> findPendingRidesByCategory(@Param("status") RideStatus status,
                                         @Param("categoryName") String categoryName);

    @Query("SELECT COUNT(r) FROM Ride r WHERE r.status = :status AND LOWER(r.pickupAddress) LIKE LOWER(CONCAT('%', :zoneName, '%'))")
    long countPendingRidesByPickupZone(@Param("status") RideStatus status, @Param("zoneName") String zoneName);

    List<Ride> findByRiderOrderByRequestedAtDesc(Rider rider);

    List<Ride> findByDriverOrderByRequestedAtDesc(Driver driver);

    @EntityGraph(attributePaths = {"fareRule", "fareRule.vehicleCategory", "driver", "driver.user", "vehicle", "rating"})
    Page<Ride> findByRider(Rider rider, Pageable pageable);

    @EntityGraph(attributePaths = {"fareRule", "fareRule.vehicleCategory", "driver", "driver.user", "vehicle", "rating"})
    Page<Ride> findByRiderAndStatus(Rider rider, RideStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"fareRule", "fareRule.vehicleCategory", "rider", "rider.user", "vehicle", "rating"})
    Page<Ride> findByDriver(Driver driver, Pageable pageable);

    @EntityGraph(attributePaths = {"fareRule", "fareRule.vehicleCategory", "rider", "rider.user", "vehicle", "rating"})
    Page<Ride> findByDriverAndStatus(Driver driver, RideStatus status, Pageable pageable);

    @Query("SELECT r FROM Ride r WHERE r.rider = :rider AND r.status IN ('PENDING', 'ACCEPTED', 'ARRIVED', 'IN_PROGRESS')")
    Optional<Ride> findActiveRideByRider(@Param("rider") Rider rider);

    @Query("SELECT r FROM Ride r WHERE r.driver = :driver AND r.status IN ('ACCEPTED', 'ARRIVED', 'IN_PROGRESS')")
    Optional<Ride> findActiveRideByDriver(@Param("driver") Driver driver);

    long countByStatus(RideStatus status);

    @Query("SELECT COALESCE(SUM(r.platformCommission), 0) FROM Ride r WHERE r.status = 'COMPLETED'")
    BigDecimal sumTotalPlatformCommission();

    @Query("SELECT r FROM Ride r JOIN FETCH r.rider rd JOIN FETCH rd.user " +
           "WHERE r.status = :status AND r.requestedAt < :cutoff")
    List<Ride> findStaleRidesByStatus(@Param("status") RideStatus status, @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE rides SET status = 'CANCELLED' WHERE ride_id = :rideId AND status = 'PENDING'", nativeQuery = true)
    int cancelRideOptimisticallyIfPending(@Param("rideId") Long rideId);
}
