package com.dropslot.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.api.dto.AuthVerifyDtos;
import com.dropslot.user.mail.InMemoryMailer;
import com.dropslot.user.test.AbstractPostgresIntegrationTest;
import java.time.Duration;
import java.util.Objects;
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
public class AuthFlowIntegrationTest extends AbstractPostgresIntegrationTest {

  @LocalServerPort int port;

  @Autowired TestRestTemplate rest;

  @Autowired InMemoryMailer mailer;

  @Test
  void fullAuthFlow() throws Exception {
    // register
    var reg = new AuthDtos.RegisterRequest("e2e@example.com", "pass1234", "E2E");
    ResponseEntity<?> r = rest.postForEntity("/auth/register", reg, Object.class);
    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();

    // send verify
    int beforeVerify = mailer.getSent().size();
    rest.postForEntity(
        "/auth/verify/send",
        new AuthVerifyDtos.SendVerifyEmailRequest("e2e@example.com"),
        Object.class);
    // wait for mailer to receive the verification message
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> mailer.getSent().size() > beforeVerify);
    assertThat(mailer.getSent()).isNotEmpty();
    String sent = mailer.getSent().get(mailer.getSent().size() - 1);
    String token = sent.substring(sent.indexOf("token=") + 6).trim();

    // verify
    rest.postForEntity(
        "/auth/verify",
        new AuthVerifyDtos.VerifyEmailRequest("e2e@example.com", token),
        Object.class);

    // login
    var login = new AuthDtos.LoginRequest("e2e@example.com", "pass1234");
    ResponseEntity<AuthDtos.TokenResponse> loginResp =
        rest.postForEntity("/auth/login", login, AuthDtos.TokenResponse.class);
    assertThat(loginResp.getStatusCode().is2xxSuccessful()).isTrue();
    AuthDtos.TokenResponse loginBody = loginResp.getBody();
    assertThat(loginBody).isNotNull();

    // refresh
    Objects.requireNonNull(loginBody);
    var refreshReq = new AuthDtos.RefreshRequest(loginBody.refreshToken());
    ResponseEntity<AuthDtos.TokenResponse> refreshResp =
        rest.postForEntity("/auth/refresh", refreshReq, AuthDtos.TokenResponse.class);
    assertThat(refreshResp.getStatusCode().is2xxSuccessful()).isTrue();

    // request reset
    int beforeReset = mailer.getSent().size();
    rest.postForEntity(
        "/auth/password/reset",
        new AuthVerifyDtos.PasswordResetRequest("e2e@example.com"),
        Object.class);
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> mailer.getSent().size() > beforeReset);
    String sentReset = mailer.getSent().get(mailer.getSent().size() - 1);
    String resetToken = sentReset.substring(sentReset.indexOf("token=") + 6).trim();

    rest.postForEntity(
        "/auth/password/reset/perform",
        new AuthVerifyDtos.PerformPasswordResetRequest("e2e@example.com", resetToken, "newpass"),
        Object.class);

    // login with new password
    var login2 = new AuthDtos.LoginRequest("e2e@example.com", "newpass");
    ResponseEntity<AuthDtos.TokenResponse> loginResp2 =
        rest.postForEntity("/auth/login", login2, AuthDtos.TokenResponse.class);
    assertThat(loginResp2.getStatusCode().is2xxSuccessful()).isTrue();
  }
}
