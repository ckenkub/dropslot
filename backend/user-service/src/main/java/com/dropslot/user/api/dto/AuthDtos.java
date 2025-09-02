package com.dropslot.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
  public static record RegisterRequest(
      @Schema(example = "user@example.com") @Email @NotBlank String email,
      @Schema(description = "Plain text password", minLength = 8)
          @NotBlank
          @Size(min = 8, max = 100)
          String password,
      @Schema(example = "Full Name") @NotBlank String name) {}

  public static record LoginRequest(
      @Schema(example = "user@example.com") @Email @NotBlank String email,
      @Schema(example = "password123") @NotBlank String password) {}

  public static record TokenResponse(
      @Schema(description = "JWT access token") String accessToken,
      @Schema(description = "Refresh token") String refreshToken,
      @Schema(description = "Token type", example = "Bearer") String tokenType,
      @Schema(description = "Access token TTL in seconds") long expiresIn) {}

  public static record RefreshRequest(
      @Schema(description = "Refresh token") @NotBlank String refreshToken) {}
}
