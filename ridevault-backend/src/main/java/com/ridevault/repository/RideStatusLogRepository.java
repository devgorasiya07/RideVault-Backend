package com.ridevault.repository;

import com.ridevault.entity.Ride;
import com.ridevault.entity.RideStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideStatusLogRepository extends JpaRepository<RideStatusLog, Long> {

    List<RideStatusLog> findByRideOrderByChangedAtAsc(Ride ride);
}
