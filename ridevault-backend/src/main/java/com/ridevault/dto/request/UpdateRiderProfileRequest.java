package com.ridevault.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRiderProfileRequest {

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Emergency contact must be a 10-digit mobile number")
    private String emergencyContact;
}
