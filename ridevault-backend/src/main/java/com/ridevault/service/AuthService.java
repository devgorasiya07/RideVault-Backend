package com.ridevault.service;

import com.ridevault.config.JwtService;
import com.ridevault.dto.request.*;
import com.ridevault.dto.response.AuthResponse;
import com.ridevault.entity.*;
import com.ridevault.entity.enums.Role;
import com.ridevault.exception.ResourceNotFoundException;
import com.ridevault.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Authentication & Registration Service.
 * Uses BCrypt password hashing (salted per-user so identical passwords work across any number of accounts)
 * alongside stateless JWT access tokens and stateful Refresh Token Rotation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RiderRepository riderRepository;
    private final DriverRepository driverRepository;
    private final VehicleCategoryRepository categoryRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository documentRepository;
    private final WalletRepository walletRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ========================================================================
    //  Rider Registration (Email + Password)
    // ========================================================================
    @Transactional
    public AuthResponse registerRider(RiderRegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String phoneNo = request.getPhoneNo().trim();
        log.info("Processing rider registration for email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered: " + email);
        }
        if (userRepository.existsByPhoneNo(phoneNo)) {
            throw new IllegalArgumentException("Phone number is already registered: " + phoneNo);
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .middleName(request.getMiddleName() != null ? request.getMiddleName().trim() : null)
                .lastName(request.getLastName().trim())
                .email(email)
                .phoneNo(phoneNo)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_RIDER)
                .isActive(true)
                .build();
        User savedUser = userRepository.save(user);

        riderRepository.save(Rider.builder()
                .user(savedUser)
                .isVerified(true)
                .build());

        return generateAuthResponse(savedUser);
    }

    // ========================================================================
    //  Driver Registration (Email + Password + Vehicle + Wallet)
    // ========================================================================
    @Transactional
    public AuthResponse registerDriver(DriverRegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String phoneNo = request.getPhoneNo().trim();
        String licenseNo = request.getLicenseNo().toUpperCase().trim();
        String plateNo = request.getPlateNo().toUpperCase().trim();
        log.info("Processing driver registration for email: {}", email);

        validateDriverUniqueness(email, phoneNo, licenseNo, plateNo);

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .middleName(request.getMiddleName() != null ? request.getMiddleName().trim() : null)
                .lastName(request.getLastName().trim())
                .email(email)
                .phoneNo(phoneNo)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_DRIVER)
                .isActive(true)
                .build();
        User savedUser = userRepository.save(user);

        provisionDriverFleet(
                savedUser, licenseNo, request.getVehicleCategory(),
                request.getVehicleModel(), request.getVehicleYear(), plateNo,
                request.getInsurancePolicyNo(), request.getInsuranceExpiryDate(), true
        );

        return generateAuthResponse(savedUser);
    }

    // ========================================================================
    //  User Login (Email + Password)
    // ========================================================================
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Authenticating user with email: {}", email);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Authentication failed for email: {}", email);
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException("User account is inactive. Please contact support.");
        }

        refreshTokenRepository.revokeAllUserTokens(user);
        return generateAuthResponse(user);
    }

    // ========================================================================
    //  JWT Refresh Token Rotation & Logout Revocation
    // ========================================================================

    @Transactional
    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        String rawRefreshToken = request.getRefreshToken().trim();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token. Please sign in again."));

        if (!storedToken.isUsable()) {
            refreshTokenRepository.revokeAllUserTokens(storedToken.getUser());
            throw new BadCredentialsException("Refresh token expired or revoked. Please sign in again.");
        }

        User user = storedToken.getUser();
        if (!jwtService.isTokenValid(rawRefreshToken, user)) {
            throw new BadCredentialsException("Refresh token signature or expiration invalid.");
        }

        refreshTokenRepository.revokeAllUserTokens(user);
        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(User user) {
        if (user != null) {
            refreshTokenRepository.revokeAllUserTokens(user);
            log.info("Revoked all refresh tokens for user: {}", user.getEmail());
        }
    }

    // ========================================================================
    //  Shared Helpers
    // ========================================================================

    private void validateDriverUniqueness(String email, String phoneNo, String licenseNo, String plateNo) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered: " + email);
        }
        if (userRepository.existsByPhoneNo(phoneNo)) {
            throw new IllegalArgumentException("Phone number is already registered: " + phoneNo);
        }
        if (driverRepository.existsByLicenseNo(licenseNo)) {
            throw new IllegalArgumentException("Driving license number is already registered: " + licenseNo);
        }
        if (vehicleRepository.existsByPlateNo(plateNo)) {
            throw new IllegalArgumentException("Vehicle plate number is already registered: " + plateNo);
        }
    }

    private void provisionDriverFleet(
            User savedUser, String licenseNo, String categoryName,
            String vehicleModel, int vehicleYear, String plateNo,
            String insurancePolicyNo, String insuranceExpiryDate, boolean verified) {

        Driver driver = Driver.builder()
                .user(savedUser)
                .licenseNo(licenseNo)
                .avgRating(new BigDecimal("5.00"))
                .totalRides(0)
                .isVerified(verified)
                .isAvailable(true)
                .build();
        Driver savedDriver = driverRepository.save(driver);

        VehicleCategory category = categoryRepository.findByCategoryNameIgnoreCase(categoryName.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle category not found: " + categoryName));

        Vehicle vehicle = Vehicle.builder()
                .driver(savedDriver)
                .category(category)
                .makeModel(vehicleModel.trim())
                .manufactureYear(vehicleYear)
                .plateNo(plateNo)
                .isActive(true)
                .isVerified(verified)
                .build();
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        LocalDate expiryDate = (insuranceExpiryDate != null && !insuranceExpiryDate.isBlank())
                ? LocalDate.parse(insuranceExpiryDate.trim())
                : LocalDate.now().plusYears(1);

        String policyNo = (insurancePolicyNo != null && !insurancePolicyNo.isBlank())
                ? insurancePolicyNo.trim()
                : "POL-" + System.currentTimeMillis();

        VehicleDocument document = VehicleDocument.builder()
                .vehicle(savedVehicle)
                .insurancePolicyNo(policyNo)
                .insuranceExpiryDate(expiryDate)
                .rcNumber("RC-" + plateNo)
                .build();
        documentRepository.save(document);

        Wallet wallet = Wallet.builder()
                .driver(savedDriver)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .updatedAt(LocalDateTime.now())
                .build();
        walletRepository.save(wallet);

        log.info("Driver fleet provisioned: driverId={}, vehicleId={}", savedDriver.getDriverId(), savedVehicle.getVehicleId());
    }

    private AuthResponse generateAuthResponse(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getUserId());
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("name", user.getFullName());

        String accessToken = jwtService.generateAccessToken(extraClaims, user);
        String refreshToken = jwtService.generateRefreshToken(user);

        RefreshToken tokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(tokenEntity);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                user.getRole().name(),
                user.getEmail(),
                user.getPhoneNo(),
                user.getFullName()
        );
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
