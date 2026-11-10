package com.ridevault.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

/**
 * Spring Security configuration — stateless JWT authentication & JSON error responses.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth
                // Stateful logout requires an authenticated principal
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

                // Public static assets, auth endpoints, OpenAPI docs, and System Hub labs
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/rider",
                    "/rider.html",
                    "/rider-portal",
                    "/driver",
                    "/driver.html",
                    "/driver-portal",
                    "/labs",
                    "/hub",
                    "/favicon.ico",
                    "/api/v1/auth/**",
                    "/api/v1/hub/**",
                    "/api/v1/labs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/static/**",
                    "/*.html",
                    "/*.css",
                    "/*.js"
                ).permitAll()

                // Rider-only endpoints
                .requestMatchers("/api/v1/rider/**").hasRole("RIDER")

                // Driver-only endpoints
                .requestMatchers("/api/v1/driver/**").hasRole("DRIVER")

                // Admin-only endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Ride endpoints with explicit role checks
                .requestMatchers(HttpMethod.POST, "/api/v1/rides/book").hasRole("RIDER")
                .requestMatchers(HttpMethod.POST, "/api/v1/rides/*/rate").hasRole("RIDER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/rides/*/accept").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/rides/*/complete").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/rides/*/status").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/rides/**").authenticated()

                // Wallet endpoints — driver only
                .requestMatchers("/api/v1/wallet/**").hasRole("DRIVER")

                .anyRequest().authenticated()
            )

            // Guarantee uniform JSON error responses for 401 and 403 (never empty bodies)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(String.format(
                        "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required or token expired. Please sign in.\"}",
                        LocalDateTime.now()
                    ));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(String.format(
                        "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied for your current role.\"}",
                        LocalDateTime.now()
                    ));
                })
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authenticationProvider(createDaoAuthenticationProvider())

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationProvider createDaoAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
