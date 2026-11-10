package com.ridevault.repository;

import com.ridevault.entity.Driver;
import com.ridevault.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByPlateNo(String plateNo);

    boolean existsByPlateNo(String plateNo);

    List<Vehicle> findByDriver(Driver driver);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.category WHERE v.driver = :driver AND v.isActive = true")
    Optional<Vehicle> findActiveVehicleByDriver(@Param("driver") Driver driver);
}
