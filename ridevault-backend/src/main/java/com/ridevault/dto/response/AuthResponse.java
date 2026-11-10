package com.ridevault.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response containing JWT tokens.
 *
 * Resume: "secured via Spring Security (JWT)"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String role;
    private String email;
    private String phoneNo;
    private String fullName;

    public static AuthResponse of(String accessToken, String refreshToken,
                                  String role, String email, String fullName) {
        return of(accessToken, refreshToken, role, email, null, fullName);
    }

    public static AuthResponse of(String accessToken, String refreshToken,
                                  String role, String email, String phoneNo, String fullName) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .role(role)
                .email(email)
                .phoneNo(phoneNo)
                .fullName(fullName)
                .build();
    }
}
