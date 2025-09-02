package com.dropslot.user.api;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.api.dto.AuthVerifyDtos;
import com.dropslot.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and account management")
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  @Operation(summary = "Register a new user")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }

  @PostMapping("/login")
  @Operation(summary = "Login and receive access & refresh tokens")
  @ApiResponse(
      responseCode = "401",
      description = "Invalid credentials or account not active",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<AuthDtos.TokenResponse> login(
      @Valid @RequestBody AuthDtos.LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refresh access token using refresh token")
  @ApiResponse(
      responseCode = "401",
      description = "Invalid or expired refresh token",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<AuthDtos.TokenResponse> refresh(
      @Valid @RequestBody AuthDtos.RefreshRequest request) {
    return ResponseEntity.ok(authService.refreshAccessToken(request.refreshToken()));
  }

  @PostMapping("/verify/send")
  @Operation(summary = "Send email verification code")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<?> sendVerification(
      @Valid @RequestBody AuthVerifyDtos.SendVerifyEmailRequest request) {
    authService.sendVerificationEmail(request.email());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/verify")
  @Operation(summary = "Verify email using a code")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid or expired verification code",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<?> verifyEmail(
      @Valid @RequestBody AuthVerifyDtos.VerifyEmailRequest request) {
    authService.verifyEmail(request.email(), request.code());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/password/reset")
  @Operation(summary = "Request a password reset token via email")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<?> requestPasswordReset(
      @Valid @RequestBody AuthVerifyDtos.PasswordResetRequest request) {
    authService.requestPasswordReset(request.email());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/password/reset/perform")
  @Operation(summary = "Perform password reset using token")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid or expired reset token",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<?> performPasswordReset(
      @Valid @RequestBody AuthVerifyDtos.PerformPasswordResetRequest request) {
    authService.performPasswordReset(request.email(), request.token(), request.newPassword());
    return ResponseEntity.ok().build();
  }
}
