package com.trimly.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    String accessToken;
    String refreshToken;       // Long-lived token for persistent session (30 days)
    long   accessTokenExpiresIn; // ms until access token expires (e.g. 86400000 = 24h)
    long   refreshTokenExpiresIn; // ms until refresh token expires (e.g. 2592000000 = 30d)
    boolean isNewUser;         // true on first-ever OTP verify — frontend can show onboarding
    UserInfo user;
}
