package com.ridevault.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Driver registration request DTO.
 * Includes personal details, license, vehicle info, and insurance — all validated.
 *
 * Resume: "@Valid DTOs, streamlining safe client integration and rejecting
 *          malformed API requests"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRegisterRequest {

    // --- Personal Details ---

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(max = 50, message = "Middle name must not exceed 50 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNo;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    // --- License ---

    @NotBlank(message = "License number is required")
    @Size(min = 5, max = 20, message = "License number must be between 5 and 20 characters")
    private String licenseNo;

    // --- Vehicle Details ---

    @NotBlank(message = "Vehicle category is required")
    @Pattern(regexp = "^(MINI|SEDAN|SUV|PREMIUM|AUTO|BIKE)$",
             message = "Vehicle category must be one of: MINI, SEDAN, SUV, PREMIUM, AUTO, BIKE")
    private String vehicleCategory;

    @NotBlank(message = "Vehicle model is required")
    @Size(max = 100, message = "Vehicle model must not exceed 100 characters")
    private String vehicleModel;

    @Min(value = 2000, message = "Vehicle year must be 2000 or later")
    @Max(value = 2030, message = "Vehicle year must be 2030 or earlier")
    private int vehicleYear;

    @NotBlank(message = "Plate number is required")
    @Size(max = 20, message = "Plate number must not exceed 20 characters")
    private String plateNo;

    // --- Insurance ---

    @NotBlank(message = "Insurance policy number is required")
    private String insurancePolicyNo;

    @NotBlank(message = "Insurance expiry date is required (YYYY-MM-DD)")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",
             message = "Insurance expiry date must be in YYYY-MM-DD format")
    private String insuranceExpiryDate;
}
