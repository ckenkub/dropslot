package com.dropslot.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public static record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String name
    ) {}

    public static record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    public static record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
    ) {}

    public static record RefreshRequest(
        @NotBlank String refreshToken
    ) {}
}
