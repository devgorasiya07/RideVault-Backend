package com.ridevault.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Fare estimate response containing breakdown by vehicle category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FareEstimateResponse {

    private String pickupAddress;
    private String dropoffAddress;
    private Double distanceKm;
    private Double surgeMultiplier;
    private Map<String, Double> faresByCategory;
}
