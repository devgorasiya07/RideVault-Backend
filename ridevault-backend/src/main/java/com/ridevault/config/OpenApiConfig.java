package com.ridevault.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI configuration.
 *
 * Swagger UI available at: http://localhost:8080/swagger-ui/index.html
 *
 * Adds a global "Authorize" button in Swagger UI for pasting JWT tokens.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rideVaultOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("RideVault API")
                        .description(
                            "Dynamic Ride Booking System — RESTful API Backend. " +
                            "Spring Boot 4 / Spring Framework 7 backend on embedded Tomcat " +
                            "with JWT authentication, 14-table BCNF PostgreSQL " +
                            "schema, ride state machine workflows, and " +
                            "@Transactional wallet settlements."
                        )
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Dev Gorasiya")
                                .url("https://github.com/devgorasiya07")
                        )
                )
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT access token here")
                        )
                );
    }
}
