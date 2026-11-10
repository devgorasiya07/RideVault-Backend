package com.ridevault.service;

import com.ridevault.entity.Driver;
import com.ridevault.entity.SurgeZone;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.entity.enums.Role;
import com.ridevault.exception.ResourceNotFoundException;
import com.ridevault.repository.DriverRepository;
import com.ridevault.repository.RideRepository;
import com.ridevault.repository.SurgeZoneRepository;
import com.ridevault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Platform Telemetry & Admin Operations Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final SurgeZoneRepository surgeZoneRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformOverview() {
        long totalRiders = userRepository.countByRole(Role.ROLE_RIDER);
        long totalDrivers = userRepository.countByRole(Role.ROLE_DRIVER);
        long onlineDrivers = driverRepository.countByIsAvailableTrue();

        long pendingRides = rideRepository.countByStatus(RideStatus.PENDING);
        long ongoingRides = rideRepository.countByStatus(RideStatus.ACCEPTED)
                + rideRepository.countByStatus(RideStatus.ARRIVED)
                + rideRepository.countByStatus(RideStatus.IN_PROGRESS);
        long completedRides = rideRepository.countByStatus(RideStatus.COMPLETED);
        long cancelledRides = rideRepository.countByStatus(RideStatus.CANCELLED);
        BigDecimal platformRevenue = rideRepository.sumTotalPlatformCommission();

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalRiders", totalRiders);
        overview.put("totalDrivers", totalDrivers);
        overview.put("onlineDrivers", onlineDrivers);
        overview.put("pendingRides", pendingRides);
        overview.put("ongoingRides", ongoingRides);
        overview.put("completedRides", completedRides);
        overview.put("cancelledRides", cancelledRides);
        overview.put("totalPlatformCommission", platformRevenue != null ? platformRevenue.doubleValue() : 0.0);
        overview.put("surgeZones", listSurgeZones());
        return overview;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSurgeZones() {
        return surgeZoneRepository.findAll(Sort.by(Sort.Direction.ASC, "zoneName")).stream()
                .map(this::mapSurgeZone)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> updateSurgeZoneMultiplier(Long surgeId, double multiplier) {
        SurgeZone zone = surgeZoneRepository.findById(surgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Surge zone not found: " + surgeId));

        BigDecimal normalized = BigDecimal.valueOf(multiplier).setScale(2, RoundingMode.HALF_UP);
        zone.setMultiplier(normalized);
        SurgeZone saved = surgeZoneRepository.save(zone);
        log.info("Updated SurgeZone '{}' multiplier to {}x", saved.getZoneName(), normalized);
        return mapSurgeZone(saved);
    }

    @Transactional
    public Map<String, Object> verifyDriver(Long driverId, boolean verified) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
        driver.setIsVerified(verified);
        Driver saved = driverRepository.save(driver);

        Map<String, Object> res = new HashMap<>();
        res.put("driverId", saved.getDriverId());
        res.put("licenseNo", saved.getLicenseNo());
        res.put("isVerified", saved.getIsVerified());
        return res;
    }

    private Map<String, Object> mapSurgeZone(SurgeZone z) {
        Map<String, Object> m = new HashMap<>();
        m.put("surgeId", z.getSurgeId());
        m.put("zoneName", z.getZoneName());
        m.put("radiusKm", z.getRadiusKm().doubleValue());
        m.put("multiplier", z.getMultiplier().doubleValue());
        m.put("surgeActive", z.getMultiplier().compareTo(BigDecimal.ONE) > 0);
        return m;
    }
}
