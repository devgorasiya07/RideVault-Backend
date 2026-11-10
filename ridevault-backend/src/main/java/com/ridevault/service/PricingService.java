package com.ridevault.service;

import com.ridevault.dto.response.FareEstimateResponse;
import com.ridevault.entity.FareRule;
import com.ridevault.entity.SurgeZone;
import com.ridevault.entity.enums.RideStatus;
import com.ridevault.repository.DriverRepository;
import com.ridevault.repository.FareRuleRepository;
import com.ridevault.repository.RideRepository;
import com.ridevault.repository.SurgeZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Pricing Engine
 *
 * Implements:
 *  1. Haversine spherical distance calculation (GPS lat/lng)
 *  2. Real-time traffic modifier
 *  3. Dynamic supply/demand + configured SurgeZone multiplier
 *  4. Single-query fare calculation across all vehicle categories (no N+1 queries)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final FareRuleRepository fareRuleRepository;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final SurgeZoneRepository surgeZoneRepository;

    // ========================================================================
    //  Multi-category fare estimates for rider route preview (Single SQL Query)
    // ========================================================================
    @Transactional(readOnly = true)
    public FareEstimateResponse estimateFares(String pickupAddress, double pickupLat, double pickupLng,
                                             String dropoffAddress, double dropoffLat, double dropoffLng) {

        double distanceKm = calculateHaversineDistance(pickupLat, pickupLng, dropoffLat, dropoffLng);
        double surgeMultiplier = calculateLiveSurgeMultiplier(pickupAddress, pickupLat, pickupLng);

        List<FareRule> fareRules = fareRuleRepository.findAllWithCategories();
        Map<String, Double> faresByCategory = new HashMap<>();

        for (FareRule rule : fareRules) {
            double fare = calculateFareWithRule(distanceKm, surgeMultiplier, rule);
            faresByCategory.put(rule.getVehicleCategory().getCategoryName(), fare);
        }

        return FareEstimateResponse.builder()
                .pickupAddress(pickupAddress)
                .dropoffAddress(dropoffAddress)
                .distanceKm(distanceKm)
                .surgeMultiplier(surgeMultiplier)
                .faresByCategory(faresByCategory)
                .build();
    }

    // ========================================================================
    //  Calculate exact fare for booking a specific category
    // ========================================================================
    @Transactional(readOnly = true)
    public double calculateFare(double pickupLat, double pickupLng,
                                double dropoffLat, double dropoffLng,
                                String pickupAddress, String categoryName) {

        double distanceKm = calculateHaversineDistance(pickupLat, pickupLng, dropoffLat, dropoffLng);
        double surgeMultiplier = calculateLiveSurgeMultiplier(pickupAddress, pickupLat, pickupLng);

        FareRule rule = fareRuleRepository.findByCategoryName(categoryName)
                .orElse(null);

        return calculateFareWithRule(distanceKm, surgeMultiplier, rule);
    }

    // ========================================================================
    //  Core Fare Formula (reads BigDecimal from entity, computes in double)
    // ========================================================================
    public double calculateFareWithRule(double distanceKm, double surgeMultiplier, FareRule rule) {
        double baseFare = (rule != null) ? rule.getBaseFare().doubleValue() : 50.00;
        double ratePerKm = (rule != null) ? rule.getPerKmRate().doubleValue() : 10.00;
        double ratePerMin = (rule != null) ? rule.getPerMinRate().doubleValue() : 1.50;
        double minimumFare = (rule != null) ? rule.getMinFare().doubleValue() : 70.00;

        double trafficModifier = 1.15;
        double estimatedTimeMin = (distanceKm / 0.5) * trafficModifier;

        double routeSubtotal = baseFare + (distanceKm * ratePerKm) + (estimatedTimeMin * ratePerMin);
        double surgedFare = routeSubtotal * surgeMultiplier;
        double finalFare = Math.max(surgedFare, minimumFare);

        return BigDecimal.valueOf(finalFare).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // ========================================================================
    //  Haversine Spherical Distance Engine (Earth radius: 6371 km)
    // ========================================================================
    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double distanceKm = Math.max(calculateRawDistanceKm(lat1, lon1, lat2, lon2), 1.5);
        return Math.round(distanceKm * 10.0) / 10.0;
    }

    private double calculateRawDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    // ========================================================================
    //  Dynamic Supply/Demand & Geographic SurgeZone Calculation
    // ========================================================================
    public double calculateLiveSurgeMultiplier(String pickupZone) {
        return calculateLiveSurgeMultiplier(pickupZone, null, null);
    }

    public double calculateLiveSurgeMultiplier(String pickupZone, Double pickupLat, Double pickupLng) {
        long activePendingRiders = rideRepository.countPendingRidesByPickupZone(RideStatus.PENDING, pickupZone);
        long availableDrivers = Math.max(driverRepository.countByIsAvailableTrue(), 1L);

        double ratio = (double) activePendingRiders / availableDrivers;
        double demandSurge = 1.0;

        if (ratio > 1.2) {
            demandSurge = 1.0 + ((ratio - 1.2) * 0.35);
        }

        double zoneMultiplier = 1.0;
        String normalizedZone = pickupZone != null ? pickupZone.trim().toLowerCase() : "";
        for (SurgeZone zone : surgeZoneRepository.findAll()) {
            if (!isZoneCurrentlyActive(zone)) continue;

            boolean nameMatches = !normalizedZone.isEmpty()
                    && (normalizedZone.contains(zone.getZoneName().toLowerCase())
                        || zone.getZoneName().toLowerCase().contains(normalizedZone));
            boolean withinRadius = pickupLat != null && pickupLng != null
                    && calculateRawDistanceKm(pickupLat, pickupLng,
                            zone.getCenterLat().doubleValue(), zone.getCenterLng().doubleValue())
                       <= zone.getRadiusKm().doubleValue();

            if (nameMatches || withinRadius) {
                zoneMultiplier = Math.max(zoneMultiplier, zone.getMultiplier().doubleValue());
            }
        }

        double effectiveSurge = Math.max(demandSurge, zoneMultiplier);
        return Math.min(Math.round(effectiveSurge * 100.0) / 100.0, 3.5);
    }

    private boolean isZoneCurrentlyActive(SurgeZone zone) {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = zone.getActiveFrom() == null || !now.isBefore(zone.getActiveFrom());
        boolean beforeEnd = zone.getActiveUntil() == null || !now.isAfter(zone.getActiveUntil());
        return afterStart && beforeEnd;
    }
}
