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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dropslot.user.util.LogUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and account management")
public class AuthController {
    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
    public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
    log.info("Register request received for email={}", LogUtils.maskEmail(request.email()));
        var resp = authService.register(request);
        log.info("User registered id={}", resp.id());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive access & refresh tokens")
    @ApiResponse(responseCode = "401", description = "Invalid credentials or account not active", content = @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
    public ResponseEntity<AuthDtos.TokenResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {
        log.info("Login attempt for email={}", LogUtils.maskEmail(request.email()));
        var tokens = authService.login(request);
        // set userId in MDC for subsequent logs in request lifecycle
        // authService.login returns tokens but we can infer userId from tokens or let
        // service set MDC
        log.info("Login success for email={}", LogUtils.maskEmail(request.email()));
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
    public ResponseEntity<AuthDtos.TokenResponse> refresh(
            @Valid @RequestBody AuthDtos.RefreshRequest request) {
        log.info("Refresh token request received");
        var tokens = authService.refreshAccessToken(request.refreshToken());
        log.info("Refresh token rotated");
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/verify/send")
    @Operation(summary = "Send email verification code")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
    public ResponseEntity<?> sendVerification(
            @Valid @RequestBody AuthVerifyDtos.SendVerifyEmailRequest request) {
    log.info("Send verification requested for email={}", LogUtils.maskEmail(request.email()));
        authService.sendVerificationEmail(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify email using a code")
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification code", content = @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
    public ResponseEntity<?> verifyEmail(
            @Valid @RequestBody AuthVerifyDtos.VerifyEmailRequest request) {
    log.info("Verify email requested for email={}", LogUtils.maskEmail(request.email()));
        authService.verifyEmail(request.email(), request.code());
    log.info("Email verified for email={}", LogUtils.maskEmail(request.email()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Request a password reset token via email")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
    public ResponseEntity<?> requestPasswordReset(
            @Valid @RequestBody AuthVerifyDtos.PasswordResetRequest request) {
    log.info("Password reset requested for email={}", LogUtils.maskEmail(request.email()));
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset/perform")
    @Operation(summary = "Perform password reset using token")
    @ApiResponse(responseCode = "400", description = "Invalid or expired reset token", content = @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
    public ResponseEntity<?> performPasswordReset(
            @Valid @RequestBody AuthVerifyDtos.PerformPasswordResetRequest request) {
    log.info("Perform password reset for email={}", LogUtils.maskEmail(request.email()));
        authService.performPasswordReset(request.email(), request.token(), request.newPassword());
    log.info("Password reset performed for email={}", LogUtils.maskEmail(request.email()));
        return ResponseEntity.ok().build();
    }
}
