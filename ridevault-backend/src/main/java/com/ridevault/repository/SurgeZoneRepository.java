package com.ridevault.repository;

import com.ridevault.entity.SurgeZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurgeZoneRepository extends JpaRepository<SurgeZone, Long> {

    Optional<SurgeZone> findByZoneNameIgnoreCase(String zoneName);
}
