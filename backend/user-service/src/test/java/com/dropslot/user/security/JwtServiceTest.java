package com.dropslot.user.security;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    @Test
    void generateAndValidateToken() {
    String b64 = Base64.getEncoder().encodeToString("test-secret-which-is-long-enough-0123456789".getBytes());
        JwtService svc = new JwtService(b64, 3600, 604800);
    String token = svc.generate("user-id-123", Map.of("roles", List.of("CUSTOMER")));
        assertNotNull(token);
        assertTrue(svc.isTokenValid(token));
        assertEquals("user-id-123", svc.extractSubject(token));
    assertEquals(List.of("CUSTOMER"), svc.extractRoles(token));
    }

    @Test
    void expiredTokenIsInvalid() throws InterruptedException {
    String b64 = Base64.getEncoder().encodeToString("test-secret-which-is-long-enough-0123456789".getBytes());
        JwtService svc = new JwtService(b64, 1, 2);
        String token = svc.generate("u2", Map.of());
        assertNotNull(token);
        // wait 2 seconds for token to expire
        Thread.sleep(2000);
        assertFalse(svc.isTokenValid(token));
    }
}
