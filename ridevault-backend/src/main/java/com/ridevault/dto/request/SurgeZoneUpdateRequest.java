package com.ridevault.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurgeZoneUpdateRequest {

    @NotNull(message = "Surge multiplier is required")
    @DecimalMin(value = "1.00", message = "Surge multiplier must be at least 1.00x")
    @DecimalMax(value = "3.50", message = "Surge multiplier cannot exceed 3.50x")
    private Double multiplier;
}
