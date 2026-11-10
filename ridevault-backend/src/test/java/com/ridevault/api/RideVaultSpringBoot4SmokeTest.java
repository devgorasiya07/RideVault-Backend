package com.ridevault.api;

import com.ridevault.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Spring Boot 4 / Spring Framework 7 Smoke Integration Test.
 * Exercises SecurityConfig (Spring Security 7), Jackson 3 serialization, JJWT 0.13.0,
 * @MapsId Hibernate 7 entities, Start PIN enforcement, 80/20 settlement, ratings, and SpringDoc 3.
 */
@SpringBootTest
public class RideVaultSpringBoot4SmokeTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Full E2E Boot 4 pipeline: Auth, Refresh Rotation, Booking, PIN Check, Settlement, Ratings & OpenAPI")
    void testEndToEndSpringBoot4Lifecycle() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        long seed = System.currentTimeMillis() % 100000000L;
        String riderEmail = "boot4_rider_" + seed + "@ridevault.in";
        String riderPhone = String.format("92%08d", seed);
        String driverEmail = "boot4_driver_" + seed + "@ridevault.in";
        String driverPhone = String.format("93%08d", seed);
        String licenseNo = "DLB4" + seed;
        String plateNo = "GJ18B4" + (seed % 10000);

        // 1. Public telemetry & OpenAPI v3 endpoints
        mockMvc.perform(get("/api/v1/hub/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        // 2. Rider register -> login -> refresh rotation
        String riderRegJson = """
                {
                  "firstName": "Aarav",
                  "lastName": "Patel",
                  "email": "%s",
                  "phoneNo": "%s",
                  "password": "Password@123"
                }
                """.formatted(riderEmail, riderPhone);

        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register/rider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(riderRegJson))
                .andExpect(status().isCreated())
                .andReturn();
        assertNotNull(jsonMapper.readTree(regRes.getResponse().getContentAsString()).path("data").path("accessToken").asString());

        String loginJson = """
                { "email": "%s", "password": "Password@123" }
                """.formatted(riderEmail);
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginData = jsonMapper.readTree(loginRes.getResponse().getContentAsString()).path("data");
        String refreshToken = loginData.path("refreshToken").asString();
        assertFalse(refreshToken.isBlank());

        String refreshJson = """
                { "refreshToken": "%s" }
                """.formatted(refreshToken);
        MvcResult refreshRes = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andReturn();
        String riderJwt = jsonMapper.readTree(refreshRes.getResponse().getContentAsString())
                .path("data").path("accessToken").asString();
        assertFalse(riderJwt.isBlank());

        // Re-using old refresh token must fail after rotation
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isUnauthorized());

        // 3. Rider profile + SOS contact update (Hibernate 7 @MapsId managed-parent check)
        mockMvc.perform(get("/api/v1/rider/me")
                        .header("Authorization", "Bearer " + riderJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(riderEmail));

        mockMvc.perform(put("/api/v1/rider/me")
                        .header("Authorization", "Bearer " + riderJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emergencyContact\":\"9876543210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emergencyContact").value("9876543210"));

        // 4. Register Driver
        String driverRegJson = """
                {
                  "firstName": "Rajesh",
                  "lastName": "Yadav",
                  "email": "%s",
                  "phoneNo": "%s",
                  "password": "Password@123",
                  "licenseNo": "%s",
                  "vehicleCategory": "SEDAN",
                  "vehicleModel": "Honda Amaze",
                  "vehicleYear": 2023,
                  "plateNo": "%s",
                  "insurancePolicyNo": "POL-B4-99",
                  "insuranceExpiryDate": "2028-01-01"
                }
                """.formatted(driverEmail, driverPhone, licenseNo, plateNo);

        MvcResult drvRegRes = mockMvc.perform(post("/api/v1/auth/register/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverRegJson))
                .andExpect(status().isCreated())
                .andReturn();
        String driverJwt = jsonMapper.readTree(drvRegRes.getResponse().getContentAsString())
                .path("data").path("accessToken").asString();

        // 5. Book ride -> accept -> arrived -> wrong PIN blocked -> valid PIN -> complete -> rate -> timeline -> logout
        String bookJson = """
                {
                  "pickupAddress": "Infocity IT Park",
                  "pickupLat": 23.1885,
                  "pickupLng": 72.6285,
                  "dropoffAddress": "GIFT City",
                  "dropoffLat": 23.1610,
                  "dropoffLng": 72.6823,
                  "vehicleCategory": "SEDAN"
                }
                """;
        MvcResult bookRes = mockMvc.perform(post("/api/v1/rides/book")
                        .header("Authorization", "Bearer " + riderJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode rideNode = jsonMapper.readTree(bookRes.getResponse().getContentAsString()).path("data");
        long rideId = rideNode.path("rideId").asLong();
        String startPin = rideNode.path("startPin").asString();
        double estimatedFare = rideNode.path("estimatedFare").asDouble();
        assertEquals(4, startPin.length());

        // Role enforcement: Rider cannot accept a ride
        mockMvc.perform(put("/api/v1/rides/" + rideId + "/accept")
                        .header("Authorization", "Bearer " + riderJwt))
                .andExpect(status().isForbidden());

        // Driver claims optimistically
        mockMvc.perform(put("/api/v1/rides/" + rideId + "/accept")
                        .header("Authorization", "Bearer " + driverJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(put("/api/v1/rides/" + rideId + "/status")
                        .header("Authorization", "Bearer " + driverJwt)
                        .param("targetStatus", "ARRIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARRIVED"));

        // Wrong PIN rejected
        mockMvc.perform(put("/api/v1/rides/" + rideId + "/status")
                        .header("Authorization", "Bearer " + driverJwt)
                        .param("targetStatus", "IN_PROGRESS")
                        .param("startPin", "0000"))
                .andExpect(status().isBadRequest());

        // Correct PIN verified -> IN_PROGRESS
        mockMvc.perform(put("/api/v1/rides/" + rideId + "/status")
                        .header("Authorization", "Bearer " + driverJwt)
                        .param("targetStatus", "IN_PROGRESS")
                        .param("startPin", startPin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // Complete & settle 80/20
        mockMvc.perform(put("/api/v1/rides/" + rideId + "/complete")
                        .header("Authorization", "Bearer " + driverJwt)
                        .param("collectedFare", String.valueOf(estimatedFare)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Rate 5 stars
        mockMvc.perform(post("/api/v1/rides/" + rideId + "/rate")
                        .header("Authorization", "Bearer " + riderJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":5,\"comment\":\"Smooth Boot 4 trip!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ratingStars").value(5));

        // Audit timeline & Paginated queries
        mockMvc.perform(get("/api/v1/rides/" + rideId + "/timeline")
                        .header("Authorization", "Bearer " + riderJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5));

        mockMvc.perform(get("/api/v1/rider/rides/paged")
                        .header("Authorization", "Bearer " + riderJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/wallet/transactions")
                        .header("Authorization", "Bearer " + driverJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // Logout revocation
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + riderJwt))
                .andExpect(status().isOk());

        assertTrue(userRepository.existsByEmail(riderEmail));
    }
}
