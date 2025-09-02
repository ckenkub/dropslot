package com.dropslot.user.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

public class JwtServiceTest {

  @Test
  void generateAndValidateToken() {
    String b64 =
        Base64.getEncoder()
            .encodeToString("test-secret-which-is-long-enough-0123456789".getBytes());
    JwtService svc = new JwtService(b64, 3600, 604800);
    String token = svc.generate("user-id-123", Map.of("roles", List.of("CUSTOMER")));
    assertNotNull(token);
    assertTrue(svc.isTokenValid(token));
    assertEquals("user-id-123", svc.extractSubject(token));
    assertEquals(List.of("CUSTOMER"), svc.extractRoles(token));
  }

  @Test
  void expiredTokenIsInvalid() throws InterruptedException {
    String b64 =
        Base64.getEncoder()
            .encodeToString("test-secret-which-is-long-enough-0123456789".getBytes());
    JwtService svc = new JwtService(b64, 1, 2);
    String token = svc.generate("u2", Map.of());
    assertNotNull(token);
    // wait until token becomes invalid (allow up to 5s)
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertFalse(svc.isTokenValid(token)));
  }
}
