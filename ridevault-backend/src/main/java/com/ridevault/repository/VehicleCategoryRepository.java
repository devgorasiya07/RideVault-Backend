package com.ridevault.repository;

import com.ridevault.entity.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleCategoryRepository extends JpaRepository<VehicleCategory, Long> {

    Optional<VehicleCategory> findByCategoryNameIgnoreCase(String categoryName);

    Optional<VehicleCategory> findByCategoryName(String categoryName);

    boolean existsByCategoryNameIgnoreCase(String categoryName);
}
