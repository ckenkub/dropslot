package com.dropslot.user.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dropslot.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AuthControllerUnitTest {
    private AuthService authService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void verifyEmail_endpoint_calls_service_and_returns_ok() throws Exception {
        String payload = "{\"email\":\"u@example.com\",\"code\":\"abc123\"}";

        mvc.perform(post("/auth/verify").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        verify(authService, times(1)).verifyEmail("u@example.com", "abc123");
    }

    @Test
    void performPasswordReset_calls_service_and_returns_ok() throws Exception {
        String payload = "{\"email\":\"u@example.com\",\"token\":\"rst-1\",\"newPassword\":\"newpass\"}";

        mvc.perform(
                post("/auth/password/reset/perform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(authService, times(1)).performPasswordReset("u@example.com", "rst-1", "newpass");
    }

    @Test
    void sendVerifyEmail_calls_service_and_returns_ok() throws Exception {
        String payload = "{\"email\":\"u@example.com\"}";

        mvc.perform(post("/auth/verify/send").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        verify(authService, times(1)).sendVerificationEmail("u@example.com");
    }

    @Test
    void sendVerifyEmail_service_throws_results_in_error_status() throws Exception {
        doThrow(new IllegalArgumentException("Invalid email"))
                .when(authService)
                .sendVerificationEmail("bad@example.com");
        String payload = "{\"email\":\"bad@example.com\"}";

        mvc.perform(post("/auth/verify/send").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void requestPasswordReset_calls_service_and_returns_ok() throws Exception {
        String payload = "{\"email\":\"u@example.com\"}";

        mvc.perform(
                post("/auth/password/reset").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        verify(authService, times(1)).requestPasswordReset("u@example.com");
    }

    @Test
    void requestPasswordReset_service_throws_results_in_error_status() throws Exception {
        doThrow(new IllegalArgumentException("No such user"))
                .when(authService)
                .requestPasswordReset("missing@example.com");
        String payload = "{\"email\":\"missing@example.com\"}";

        mvc.perform(
                post("/auth/password/reset").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().is5xxServerError());
    }
}
