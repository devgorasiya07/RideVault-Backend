package com.ridevault.repository;

import com.ridevault.entity.FareRule;
import com.ridevault.entity.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareRuleRepository extends JpaRepository<FareRule, Long> {

    Optional<FareRule> findByVehicleCategory(VehicleCategory category);

    @Query("SELECT f FROM FareRule f JOIN FETCH f.vehicleCategory")
    List<FareRule> findAllWithCategories();

    @Query("SELECT f FROM FareRule f JOIN FETCH f.vehicleCategory c WHERE UPPER(c.categoryName) = UPPER(:categoryName)")
    Optional<FareRule> findByCategoryName(@Param("categoryName") String categoryName);
}
