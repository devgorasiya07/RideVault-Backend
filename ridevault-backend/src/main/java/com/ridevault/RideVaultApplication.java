package com.ridevault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RideVault — Dynamic Ride Booking System
 *
 * 3-tier Spring Boot backend on embedded Apache Tomcat exposing RESTful APIs
 * secured via Spring Security (JWT), backed by a 14-table BCNF PostgreSQL
 * schema via Spring Data JPA.
 */
@SpringBootApplication
@EnableScheduling
public class RideVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideVaultApplication.class, args);
    }
}
