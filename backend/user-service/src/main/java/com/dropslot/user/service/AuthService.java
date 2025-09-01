package com.dropslot.user.service;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.api.dto.UserProfileDto;
import com.dropslot.user.domain.Role;
import com.dropslot.user.domain.User;
import com.dropslot.user.repo.RoleRepository;
import com.dropslot.user.repo.UserRepository;
import com.dropslot.user.security.JwtService;
import com.dropslot.user.repo.RefreshTokenRepository;
import com.dropslot.user.domain.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    // In-memory token stores for scaffolding (not for production)
    private final ConcurrentMap<String,String> verificationTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String,String> passwordResetTokens = new ConcurrentHashMap<>();

    @Transactional
    public UserProfileDto register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().code("CUSTOMER").name("Customer").build()));
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        user.getRoles().add(customerRole);
        userRepository.save(user);
        return toProfile(user);
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
    Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    String accessToken = jwtService.generate(user.getId().toString(), Map.of("roles", roles));
    String jti = UUID.randomUUID().toString();
    String refreshToken = jwtService.generateRefreshToken(user.getId().toString(), jti);

    // persist refresh token record
        RefreshToken tokenEntity = RefreshToken.builder()
            .id(UUID.randomUUID())
            .userId(user.getId())
            .jti(jti)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
            .revoked(false)
            .build();
        refreshTokenRepository.save(tokenEntity);

    return new AuthDtos.TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getTtlSeconds());
    }

    public UserProfileDto toProfile(User user) {
        return new UserProfileDto(
                user.getId() != null ? user.getId().toString() : null,
                user.getEmail(),
                user.getName(),
                user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet())
        );
    }

    // --- Email verification & password reset (scaffold) ---
    public void sendVerificationEmail(String email) {
        // generate a simple token and store it in-memory
        String token = UUID.randomUUID().toString().substring(0,8);
        verificationTokens.put(email.toLowerCase(), token);
        // In a real implementation we'd enqueue/send email. For CI we just log.
        System.out.println("[scaffold] sendVerificationEmail: " + email + " token=" + token);
    }

    public void verifyEmail(String email, String code) {
        String stored = verificationTokens.get(email.toLowerCase());
        if (stored == null || !stored.equals(code)) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        // mark verified: here we can update user status if present
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setStatus("ACTIVE");
            userRepository.save(u);
        });
        verificationTokens.remove(email.toLowerCase());
    }

    public void requestPasswordReset(String email) {
        String token = UUID.randomUUID().toString().substring(0,8);
        passwordResetTokens.put(email.toLowerCase(), token);
        System.out.println("[scaffold] requestPasswordReset: " + email + " token=" + token);
    }

    public void performPasswordReset(String email, String token, String newPassword) {
        String stored = passwordResetTokens.get(email.toLowerCase());
        if (stored == null || !stored.equals(token)) {
            throw new IllegalArgumentException("Invalid password reset token");
        }
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(u);
        });
        passwordResetTokens.remove(email.toLowerCase());
    }

    @Transactional
    public AuthDtos.TokenResponse refreshAccessToken(String refreshToken) {
    if (!jwtService.isTokenValid(refreshToken)) {
        throw new IllegalArgumentException("Invalid refresh token");
    }
    String jti = jwtService.extractJti(refreshToken);
    if (jti == null) throw new IllegalArgumentException("Invalid refresh token (missing jti)");

    var stored = refreshTokenRepository.findByJti(jti)
        .orElseThrow(() -> new IllegalArgumentException("Refresh token not found or already used"));
    if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
        throw new IllegalArgumentException("Refresh token expired or revoked");
    }

    // rotate: create new jti and token, mark old revoked and linked
    String newJti = UUID.randomUUID().toString();
    String userId = jwtService.extractSubject(refreshToken);
    User user = userRepository.findById(UUID.fromString(userId))
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    String accessToken = jwtService.generate(user.getId().toString(), Map.of("roles", roles));
    String newRefresh = jwtService.generateRefreshToken(user.getId().toString(), newJti);

    // update old token
    stored.setRevoked(true);
    stored.setReplacedByJti(newJti);
    refreshTokenRepository.save(stored);

    // persist new token
    RefreshToken newEntity = RefreshToken.builder()
        .id(UUID.randomUUID())
        .userId(user.getId())
        .jti(newJti)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
        .revoked(false)
        .build();
    refreshTokenRepository.save(newEntity);

    return new AuthDtos.TokenResponse(accessToken, newRefresh, "Bearer", jwtService.getTtlSeconds());
    }
}
