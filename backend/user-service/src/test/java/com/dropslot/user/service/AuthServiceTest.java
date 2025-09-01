package com.dropslot.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.domain.RefreshToken;
import com.dropslot.user.domain.Role;
import com.dropslot.user.domain.User;
import com.dropslot.user.mail.Mailer;
import com.dropslot.user.repo.RefreshTokenRepository;
import com.dropslot.user.repo.UserRepository;
import com.dropslot.user.repo.VerificationTokenRepository;
import com.dropslot.user.security.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  private UserRepository userRepository;
  private com.dropslot.user.repo.RoleRepository roleRepository;
  private PasswordEncoder passwordEncoder;
  private JwtService jwtService;
  private RefreshTokenRepository refreshTokenRepository;
  private VerificationTokenRepository verificationTokenRepository;
  private Mailer mailer;
  private AuthService authService;

  @BeforeEach
  void setup() {
    userRepository = mock(UserRepository.class);
    roleRepository = mock(com.dropslot.user.repo.RoleRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    jwtService = mock(JwtService.class);
    refreshTokenRepository = mock(RefreshTokenRepository.class);
    verificationTokenRepository = mock(VerificationTokenRepository.class);
    mailer = mock(Mailer.class);
    authService =
        new AuthService(
            userRepository,
            roleRepository,
            passwordEncoder,
            jwtService,
            refreshTokenRepository,
            verificationTokenRepository,
            mailer);
  }

  @Test
  void refreshRotatesTokensSuccessfully() {
    String oldJti = "old-jti-1";
    String refreshJwt = "refresh.jwt.token";
    String userId = UUID.randomUUID().toString();

    RefreshToken stored =
        RefreshToken.builder()
            .id(UUID.randomUUID())
            .userId(UUID.fromString(userId))
            .jti(oldJti)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

    when(jwtService.isTokenValid(refreshJwt)).thenReturn(true);
    when(jwtService.extractJti(refreshJwt)).thenReturn(oldJti);
    when(refreshTokenRepository.findByJti(oldJti)).thenReturn(Optional.of(stored));
    when(jwtService.extractSubject(refreshJwt)).thenReturn(userId);

    User user =
        User.builder()
            .id(UUID.fromString(userId))
            .email("u@example.com")
            .name("u")
            .passwordHash("hash")
            .build();
    user.getRoles().add(Role.builder().code("CUSTOMER").name("Customer").build());

    when(userRepository.findById(UUID.fromString(userId))).thenReturn(Optional.of(user));

    when(jwtService.generate(anyString(), anyMap())).thenReturn("new.access.token");
    when(jwtService.generateRefreshToken(anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              // return token containing new jti
              return "refresh-token-for-" + invocation.getArgument(1);
            });

    // capture saved entities
    ArgumentCaptor<RefreshToken> savedCaptor = ArgumentCaptor.forClass(RefreshToken.class);

    // perform
    AuthDtos.TokenResponse resp = authService.refreshAccessToken(refreshJwt);
    assertNotNull(resp);
    assertNotNull(resp.accessToken());
    assertNotNull(resp.refreshToken());

    // verify old token was revoked and saved, and new token saved
    verify(refreshTokenRepository, times(2)).save(savedCaptor.capture());
    List<RefreshToken> saved = savedCaptor.getAllValues();
    // first save may be update of old token (revoked)
    RefreshToken first = saved.get(0);
    assertTrue(first.isRevoked());
    assertNotNull(first.getReplacedByJti());

    RefreshToken second = saved.get(1);
    assertFalse(second.isRevoked());
    assertNotNull(second.getJti());
  }

  @Test
  void refreshWithRevokedTokenIsRejected() {
    String oldJti = "old-jti-2";
    String refreshJwt = "refresh.jwt.token2";

    RefreshToken stored =
        RefreshToken.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .jti(oldJti)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(true)
            .build();

    when(jwtService.isTokenValid(refreshJwt)).thenReturn(true);
    when(jwtService.extractJti(refreshJwt)).thenReturn(oldJti);
    when(refreshTokenRepository.findByJti(oldJti)).thenReturn(Optional.of(stored));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> authService.refreshAccessToken(refreshJwt));
    assertTrue(
        ex.getMessage().toLowerCase().contains("expired")
            || ex.getMessage().toLowerCase().contains("revoked"));
  }

  @Test
  void verifyEmailMarksUserVerified() {
    String email = "v@example.com";
    String code = "abc123";
    var vt =
        com.dropslot.user.domain.VerificationToken.builder()
            .id(UUID.randomUUID())
            .email(email)
            .token(code)
            .type("VERIFY")
            .build();
    when(verificationTokenRepository.findByEmailAndType(email.toLowerCase(), "VERIFY"))
        .thenReturn(java.util.Optional.of(vt));
    User user = User.builder().id(UUID.randomUUID()).email(email).build();
    when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(user));

    authService.verifyEmail(email, code);

    verify(userRepository).save(any(User.class));
    verify(verificationTokenRepository).deleteByEmailAndType(email.toLowerCase(), "VERIFY");
  }

  @Test
  void passwordResetFlow() {
    String email = "r@example.com";
    // request
    authService.requestPasswordReset(email);
    verify(verificationTokenRepository).deleteByEmailAndType(email.toLowerCase(), "RESET");

    // simulate token stored and perform reset
    String token = "tok123";
    var vt =
        com.dropslot.user.domain.VerificationToken.builder()
            .id(UUID.randomUUID())
            .email(email)
            .token(token)
            .type("RESET")
            .build();
    when(verificationTokenRepository.findByEmailAndType(email.toLowerCase(), "RESET"))
        .thenReturn(java.util.Optional.of(vt));
    User user = User.builder().id(UUID.randomUUID()).email(email).passwordHash("old").build();
    when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(user));

    authService.performPasswordReset(email, token, "newpass");

    verify(userRepository).save(any(User.class));
    // delete called once during request and once during perform
    verify(verificationTokenRepository, times(2))
        .deleteByEmailAndType(email.toLowerCase(), "RESET");
  }
}
