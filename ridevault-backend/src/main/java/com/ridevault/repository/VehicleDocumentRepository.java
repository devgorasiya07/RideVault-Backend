package com.ridevault.repository;

import com.ridevault.entity.Vehicle;
import com.ridevault.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    Optional<VehicleDocument> findByVehicle(Vehicle vehicle);

    Optional<VehicleDocument> findByInsurancePolicyNo(String insurancePolicyNo);
}
