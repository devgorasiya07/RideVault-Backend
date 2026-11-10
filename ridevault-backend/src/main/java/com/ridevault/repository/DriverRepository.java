package com.ridevault.repository;

import com.ridevault.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByLicenseNo(String licenseNo);

    boolean existsByLicenseNo(String licenseNo);

    long countByIsAvailableTrue();

    @Query("SELECT d FROM Driver d JOIN FETCH d.user u WHERE u.email = :email")
    Optional<Driver> findByEmail(@Param("email") String email);
}
