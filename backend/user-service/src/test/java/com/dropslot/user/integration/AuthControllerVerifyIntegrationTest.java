package com.dropslot.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.api.dto.AuthVerifyDtos;
import com.dropslot.user.mail.InMemoryMailer;
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
public class AuthControllerVerifyIntegrationTest extends AbstractPostgresIntegrationTest {

  @LocalServerPort int port;

  @Autowired TestRestTemplate rest;

  @Autowired InMemoryMailer mailer;

  @Test
  void verifyThenLoginSucceeds() throws Exception {
    String email = "verify@example.com";

    // register
    var reg = new AuthDtos.RegisterRequest(email, "pass1234", "Verify User");
    ResponseEntity<?> r = rest.postForEntity("/auth/register", reg, Object.class);
    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();

    // request send verify
    rest.postForEntity(
        "/auth/verify/send", new AuthVerifyDtos.SendVerifyEmailRequest(email), Object.class);

    // wait for mail to be sent and read last sent token
    int before = mailer.getSent().size();
    rest.postForEntity(
        "/auth/verify/send", new AuthVerifyDtos.SendVerifyEmailRequest(email), Object.class);
    Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> mailer.getSent().size() > before);
    assertThat(mailer.getSent()).isNotEmpty();
    String sent = mailer.getSent().get(mailer.getSent().size() - 1);
    String token = sent.substring(sent.indexOf("token=") + 6).trim();

    // verify
    rest.postForEntity(
        "/auth/verify", new AuthVerifyDtos.VerifyEmailRequest(email, token), Object.class);

    // login should now succeed
    var login = new AuthDtos.LoginRequest(email, "pass1234");
    ResponseEntity<AuthDtos.TokenResponse> loginResp =
        rest.postForEntity("/auth/login", login, AuthDtos.TokenResponse.class);
    assertThat(loginResp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(loginResp.getBody()).isNotNull();
  }
}
