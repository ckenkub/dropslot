package com.dropslot.user.service;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.api.dto.UserProfileDto;
import com.dropslot.user.domain.RefreshToken;
import com.dropslot.user.domain.Role;
import com.dropslot.user.domain.User;
import com.dropslot.user.domain.VerificationToken;
import com.dropslot.user.mail.Mailer;
import com.dropslot.user.repo.RefreshTokenRepository;
import com.dropslot.user.repo.RoleRepository;
import com.dropslot.user.repo.UserRepository;
import com.dropslot.user.repo.VerificationTokenRepository;
import com.dropslot.user.security.JwtService;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final Mailer mailer;

  @Transactional
  public UserProfileDto register(AuthDtos.RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("Email already registered");
    }
    Role customerRole =
        roleRepository
            .findByCode("CUSTOMER")
            .orElseGet(
                () ->
                    roleRepository.save(Role.builder().code("CUSTOMER").name("Customer").build()));
    User user =
        User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .name(request.name())
            .status("PENDING")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    user.getRoles().add(customerRole);
    userRepository.save(user);
    return toProfile(user);
  }

  @Transactional
  public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials");
    }
    if (!"ACTIVE".equals(user.getStatus())) {
      throw new IllegalArgumentException(
          "Account not active. Please verify your email before logging in.");
    }
    Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    String accessToken = jwtService.generate(user.getId().toString(), Map.of("roles", roles));
    String jti = UUID.randomUUID().toString();
    String refreshToken = jwtService.generateRefreshToken(user.getId().toString(), jti);

    // persist refresh token record
    RefreshToken tokenEntity =
        RefreshToken.builder()
            .id(UUID.randomUUID())
            .userId(user.getId())
            .jti(jti)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
            .revoked(false)
            .build();
    refreshTokenRepository.save(tokenEntity);

    return new AuthDtos.TokenResponse(
        accessToken, refreshToken, "Bearer", jwtService.getTtlSeconds());
  }

  public UserProfileDto toProfile(User user) {
    return new UserProfileDto(
        user.getId() != null ? user.getId().toString() : null,
        user.getEmail(),
        user.getName(),
        user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()));
  }

  // --- Email verification & password reset (scaffold) ---
  @Transactional
  public void sendVerificationEmail(String email) {
    String token = UUID.randomUUID().toString().substring(0, 8);
    VerificationToken vt =
        VerificationToken.builder()
            .id(UUID.randomUUID())
            .email(email.toLowerCase())
            .token(token)
            .type("VERIFY")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    verificationTokenRepository.deleteByEmailAndType(email.toLowerCase(), "VERIFY");
    verificationTokenRepository.save(vt);
    mailer.send(email, "Verify your account", "Your verification code: " + token);
  }

  @Transactional
  public void verifyEmail(String email, String code) {
    var opt = verificationTokenRepository.findByEmailAndType(email.toLowerCase(), "VERIFY");
    if (opt.isEmpty()) {
      throw new IllegalArgumentException("Invalid verification code");
    }
    var vt = opt.get();
    if (vt.getExpiresAt() != null && vt.getExpiresAt().isBefore(Instant.now())) {
      // cleanup expired token
      verificationTokenRepository.deleteByEmailAndType(email.toLowerCase(), "VERIFY");
      throw new IllegalArgumentException("Verification code expired");
    }
    if (!vt.getToken().equals(code)) {
      throw new IllegalArgumentException("Invalid verification code");
    }
    userRepository
        .findByEmail(email)
        .ifPresent(
            u -> {
              u.setStatus("ACTIVE");
              userRepository.save(u);
            });
    verificationTokenRepository.deleteByEmailAndType(email.toLowerCase(), "VERIFY");
  }

  @Transactional
  public void requestPasswordReset(String email) {
    String token = UUID.randomUUID().toString().substring(0, 8);
    VerificationToken vt =
        VerificationToken.builder()
            .id(UUID.randomUUID())
            .email(email.toLowerCase())
            .token(token)
            .type("RESET")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    verificationTokenRepository.deleteByEmailAndType(email.toLowerCase(), "RESET");
    verificationTokenRepository.save(vt);
    mailer.send(email, "Password reset", "Your password reset token: " + token);
  }

  @Transactional
  public void performPasswordReset(String email, String token, String newPassword) {
    var opt = verificationTokenRepository.findByEmailAndType(email.toLowerCase(), "RESET");
    if (opt.isEmpty()) {
      throw new IllegalArgumentException("Invalid password reset token");
    }
    var vt = opt.get();
    if (vt.getExpiresAt() != null && vt.getExpiresAt().isBefore(Instant.now())) {
      verificationTokenRepository.deleteByEmailAndType(email.toLowerCase(), "RESET");
      throw new IllegalArgumentException("Password reset token expired");
    }
    if (!vt.getToken().equals(token)) {
      throw new IllegalArgumentException("Invalid password reset token");
    }
    userRepository
        .findByEmail(email)
        .ifPresent(
            u -> {
              u.setPasswordHash(passwordEncoder.encode(newPassword));
              userRepository.save(u);
            });
    verificationTokenRepository.deleteByEmailAndType(email.toLowerCase(), "RESET");
  }

  @Transactional
  public AuthDtos.TokenResponse refreshAccessToken(String refreshToken) {
    if (!jwtService.isTokenValid(refreshToken)) {
      throw new IllegalArgumentException("Invalid refresh token");
    }
    String jti = jwtService.extractJti(refreshToken);
    if (jti == null) throw new IllegalArgumentException("Invalid refresh token (missing jti)");

    var stored =
        refreshTokenRepository
            .findByJti(jti)
            .orElseThrow(
                () -> new IllegalArgumentException("Refresh token not found or already used"));
    if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
      throw new IllegalArgumentException("Refresh token expired or revoked");
    }

    // rotate: create new jti and token, mark old revoked and linked
    String newJti = UUID.randomUUID().toString();
    String userId = jwtService.extractSubject(refreshToken);
    User user =
        userRepository
            .findById(UUID.fromString(userId))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    String accessToken = jwtService.generate(user.getId().toString(), Map.of("roles", roles));
    String newRefresh = jwtService.generateRefreshToken(user.getId().toString(), newJti);

    // update old token
    stored.setRevoked(true);
    stored.setReplacedByJti(newJti);
    refreshTokenRepository.save(stored);

    // persist new token
    RefreshToken newEntity =
        RefreshToken.builder()
            .id(UUID.randomUUID())
            .userId(user.getId())
            .jti(newJti)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
            .revoked(false)
            .build();
    refreshTokenRepository.save(newEntity);

    return new AuthDtos.TokenResponse(
        accessToken, newRefresh, "Bearer", jwtService.getTtlSeconds());
  }
}
