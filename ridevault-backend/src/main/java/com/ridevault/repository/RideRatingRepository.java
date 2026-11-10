package com.ridevault.repository;

import com.ridevault.entity.Ride;
import com.ridevault.entity.RideRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RideRatingRepository extends JpaRepository<RideRating, Long> {

    boolean existsByRide(Ride ride);

    Optional<RideRating> findByRide(Ride ride);

    @Query("SELECT AVG(rr.stars) FROM RideRating rr WHERE rr.ride.driver.driverId = :driverId")
    Double calculateAverageStarsByDriverId(@Param("driverId") Long driverId);
}
