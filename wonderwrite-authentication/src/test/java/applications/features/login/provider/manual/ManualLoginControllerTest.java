package applications.features.login.provider.manual;

import applications.extensions.exception.RateLimitedException;
import applications.security.AuthCookieWriter;
import applications.security.AuthExceptionAdvice;
import applications.utils.tokens.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ManualLoginControllerTest {

    @Mock
    private ManualLoginService manualLoginService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ManualLoginController(manualLoginService, new AuthCookieWriter()))
                .setControllerAdvice(new AuthExceptionAdvice())
                .build();
    }

    @Test
    void verifiedAccountLoginReturns200WithCookiesAndNoOtp() throws Exception {
        UUID id = UUID.randomUUID();
        when(manualLoginService.execute(any())).thenReturn(new ManualLoginResult.Success(
                id, "user@example.com", new TokenPair("access-value", "refresh-value", 900, 604800)));

        MvcResult result = mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "S3cret!passphrase"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.otpRequired").value(false))
                .andExpect(jsonPath("$.otpTtlSeconds").value(nullValue()))
                .andReturn();

        assertThat(result.getResponse().getHeaders("Set-Cookie")).hasSize(2);
        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("access_token=access-value", "HttpOnly", "Secure", "SameSite=Strict", "Max-Age=900");
    }

    @Test
    void unverifiedAccountLoginReturns200OtpChallengeWithoutCookies() throws Exception {
        UUID id = UUID.randomUUID();
        when(manualLoginService.execute(any())).thenReturn(
                new ManualLoginResult.OtpChallenge(id, "newuser@example.com", 300));

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newuser@example.com",
                                  "password": "S3cret!passphrase"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.otpRequired").value(true))
                .andExpect(jsonPath("$.otpTtlSeconds").value(300))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void invalidCredentialsReturn401WithEmptyBody() throws Exception {
        when(manualLoginService.execute(any())).thenReturn(
                new ManualLoginResult.Failed("Invalid email or password"));

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void rateLimitedLoginBecomes429WithRetryAfter() throws Exception {
        when(manualLoginService.execute(any())).thenThrow(
                new RateLimitedException("Too many failed login attempts", Duration.ofMinutes(15)));

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "S3cret!passphrase"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.reason").value("RATE_LIMITED"));
    }

    @Test
    void malformedEmailIsRejectedByBeanValidation() throws Exception {
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "S3cret!passphrase"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("email")));
    }
}
