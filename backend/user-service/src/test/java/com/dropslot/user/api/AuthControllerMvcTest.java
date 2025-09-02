package com.dropslot.user.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dropslot.user.api.dto.AuthDtos;
import com.dropslot.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AuthControllerMvcTest {

  @Test
  void controllerPropagatesServiceException() {
    AuthService svc = Mockito.mock(AuthService.class);
    AuthController c = new AuthController(svc);

    Mockito.when(svc.login(Mockito.any()))
        .thenThrow(new IllegalArgumentException("Account not active"));

    var req = new AuthDtos.LoginRequest("x@example.com", "pass1234");
    assertThatThrownBy(() -> c.login(req))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Account not active");
  }
}
