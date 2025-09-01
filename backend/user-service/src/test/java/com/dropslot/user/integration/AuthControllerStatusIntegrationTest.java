package com.dropslot.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.domain.User;
import com.dropslot.user.repo.UserRepository;
import com.dropslot.user.test.AbstractPostgresIntegrationTest;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AuthControllerStatusIntegrationTest extends AbstractPostgresIntegrationTest {

  @LocalServerPort int port;

  @Autowired TestRestTemplate rest;

  @Autowired UserRepository userRepository;

  @Test
  void registerSetsPendingAndLoginRejectedWhenNotActive() throws Exception {
    String email = "pending@example.com";

    // register
    var reg = new AuthDtos.RegisterRequest(email, "pass1234", "Pending User");
    ResponseEntity<?> r = rest.postForEntity("/auth/register", reg, Object.class);
    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();

    // verify user stored as PENDING (await until visible)
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              User user = userRepository.findByEmail(email).orElseThrow();
              assertThat(user.getStatus()).isEqualTo("PENDING");
            });

    // attempt login before verification -> should not be successful
    var login = new AuthDtos.LoginRequest(email, "pass1234");
    ResponseEntity<?> loginResp = rest.postForEntity("/auth/login", login, Object.class);
    assertThat(loginResp.getStatusCode().is2xxSuccessful()).isFalse();
  }
}
