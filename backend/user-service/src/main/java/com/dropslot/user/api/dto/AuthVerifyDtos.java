package com.dropslot.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthVerifyDtos {
  public static record VerifyEmailRequest(
      @Schema(example = "user@example.com") @Email @NotBlank String email,
      @Schema(example = "abc12345") @NotBlank String code) {}

  public static record SendVerifyEmailRequest(
      @Schema(example = "user@example.com") @Email @NotBlank String email) {}

  public static record PasswordResetRequest(
      @Schema(example = "user@example.com") @Email @NotBlank String email) {}

  public static record PerformPasswordResetRequest(
      @Schema(example = "user@example.com") @Email @NotBlank String email,
      @Schema(example = "rst-1") @NotBlank String token,
      @Schema(description = "New plain text password", minLength = 8) @NotBlank
          String newPassword) {}
}
