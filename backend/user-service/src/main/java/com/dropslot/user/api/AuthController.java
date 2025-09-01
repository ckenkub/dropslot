package com.dropslot.user.api;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.api.dto.AuthVerifyDtos;
import com.dropslot.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthDtos.TokenResponse> login(
      @Valid @RequestBody AuthDtos.LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthDtos.TokenResponse> refresh(
      @Valid @RequestBody AuthDtos.RefreshRequest request) {
    return ResponseEntity.ok(authService.refreshAccessToken(request.refreshToken()));
  }

  @PostMapping("/verify/send")
  public ResponseEntity<?> sendVerification(
      @Valid @RequestBody AuthVerifyDtos.SendVerifyEmailRequest request) {
    authService.sendVerificationEmail(request.email());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/verify")
  public ResponseEntity<?> verifyEmail(
      @Valid @RequestBody AuthVerifyDtos.VerifyEmailRequest request) {
    authService.verifyEmail(request.email(), request.code());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/password/reset")
  public ResponseEntity<?> requestPasswordReset(
      @Valid @RequestBody AuthVerifyDtos.PasswordResetRequest request) {
    authService.requestPasswordReset(request.email());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/password/reset/perform")
  public ResponseEntity<?> performPasswordReset(
      @Valid @RequestBody AuthVerifyDtos.PerformPasswordResetRequest request) {
    authService.performPasswordReset(request.email(), request.token(), request.newPassword());
    return ResponseEntity.ok().build();
  }
}
