package com.dropslot.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthVerifyDtos {
    public static record VerifyEmailRequest(
        @Email @NotBlank String email,
        @NotBlank String code
    ) {}

    public static record SendVerifyEmailRequest(
        @Email @NotBlank String email
    ) {}

    public static record PasswordResetRequest(
        @Email @NotBlank String email
    ) {}

    public static record PerformPasswordResetRequest(
        @Email @NotBlank String email,
        @NotBlank String token,
        @NotBlank String newPassword
    ) {}
}
